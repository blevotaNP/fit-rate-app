package com.outfitrating;

import static org.assertj.core.api.Assertions.assertThat;

import com.outfitrating.dto.AuthResponse;
import com.outfitrating.dto.CreateLookRequest;
import com.outfitrating.dto.LookDetailDto;
import com.outfitrating.dto.LoginRequest;
import com.outfitrating.dto.RegisterRequest;
import com.outfitrating.dto.ReviewDto;
import com.outfitrating.dto.SaveReviewRequest;
import com.outfitrating.domain.Category;
import com.outfitrating.domain.LookType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Walks the path a real member takes: register, publish, rate, and watch the look's averages
 * change. Anything that breaks the contract between the controllers, the security filter and the
 * score aggregation shows up here.
 */
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RatingFlowIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("a published look collects verdicts and its averages follow them")
    void endToEndRatingFlow() {
        String authorToken = register("curator_one", "curator.one@example.test");
        String raterToken = register("rater_one", "rater.one@example.test");

        // --- publish ---------------------------------------------------------
        CreateLookRequest request = new CreateLookRequest(
                "Charcoal Overcoat Study",
                LookType.OUTFIT,
                Category.MINIMAL,
                null,
                null,
                "A test submission.",
                "https://example.test/coat.jpg",
                "Test Label",
                24900,
                "EUR",
                null,
                List.of("minimal", "outerwear"));

        ResponseEntity<LookDetailDto> created = rest.exchange(
                "/api/looks", HttpMethod.POST, entity(request, authorToken), LookDetailDto.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LookDetailDto look = created.getBody();
        assertThat(look).isNotNull();
        assertThat(look.slug()).isEqualTo("charcoal-overcoat-study");
        assertThat(look.ratingsCount()).isZero();
        assertThat(look.tags()).hasSize(2);

        // --- rate ------------------------------------------------------------
        SaveReviewRequest verdict = new SaveReviewRequest(
                (short) 9, (short) 8, (short) 9, (short) 7, (short) 7, "Sharp", "Reads well.");

        ResponseEntity<ReviewDto> rated = rest.exchange(
                "/api/looks/" + look.slug() + "/review", HttpMethod.PUT,
                entity(verdict, raterToken), ReviewDto.class);

        assertThat(rated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rated.getBody()).isNotNull();
        // (9 + 8 + 9 + 7 + 7) = 40 of 50 -> 80
        assertThat(rated.getBody().score()).isEqualTo((short) 80);

        LookDetailDto afterFirst = fetch(look.slug(), null);
        assertThat(afterFirst.ratingsCount()).isEqualTo(1);
        assertThat(afterFirst.reviewsCount()).isEqualTo(1);
        assertThat(afterFirst.scores().overall()).isEqualTo(80.0);
        assertThat(afterFirst.scores().style()).isEqualTo(9.0);

        // --- re-rate: one verdict per person, so this replaces rather than adds ---
        SaveReviewRequest revised = new SaveReviewRequest(
                (short) 5, (short) 5, (short) 5, (short) 5, (short) 5, null, null);
        rest.exchange("/api/looks/" + look.slug() + "/review", HttpMethod.PUT,
                entity(revised, raterToken), ReviewDto.class);

        LookDetailDto afterRevision = fetch(look.slug(), null);
        assertThat(afterRevision.ratingsCount()).isEqualTo(1);
        assertThat(afterRevision.reviewsCount()).isZero();
        assertThat(afterRevision.scores().overall()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("you cannot rate a look you submitted yourself")
    void authorCannotRateOwnLook() {
        String token = register("solo_author", "solo.author@example.test");

        LookDetailDto look = rest.exchange("/api/looks", HttpMethod.POST,
                entity(new CreateLookRequest("Self Rated Attempt", LookType.ITEM, Category.DENIM,
                        null, null, null, "https://example.test/x.jpg", null, null, null, null,
                        List.of()), token),
                LookDetailDto.class).getBody();
        assertThat(look).isNotNull();

        ResponseEntity<String> response = rest.exchange(
                "/api/looks/" + look.slug() + "/review", HttpMethod.PUT,
                entity(new SaveReviewRequest((short) 9, (short) 9, (short) 9, (short) 9, (short) 9,
                        null, null), token),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("writes require a bearer token, reads do not")
    void writesAreProtected() {
        assertThat(rest.getForEntity("/api/looks", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        ResponseEntity<String> anonymousWrite = rest.exchange("/api/looks", HttpMethod.POST,
                entity(new CreateLookRequest("Anonymous", LookType.ITEM, Category.DENIM, null, null,
                        null, "https://example.test/x.jpg", null, null, null, null, List.of()), null),
                String.class);

        assertThat(anonymousWrite.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a duplicate username is rejected with a conflict, not a stack trace")
    void duplicateUsernameConflicts() {
        register("taken_name", "taken.name@example.test");

        ResponseEntity<String> second = rest.postForEntity("/api/auth/register",
                new RegisterRequest("taken_name", "other@example.test", "password123", null),
                String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("validation failures name the offending field")
    void validationReportsFieldErrors() {
        ResponseEntity<String> response = rest.postForEntity("/api/auth/register",
                new RegisterRequest("x", "not-an-email", "short", null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("fieldErrors");
    }

    @Test
    void signInAcceptsEitherUsernameOrEmail() {
        register("dual_login", "dual.login@example.test");

        assertThat(login("dual_login", "password123")).isNotBlank();
        assertThat(login("dual.login@example.test", "password123")).isNotBlank();
    }

    // --- helpers ---------------------------------------------------------------

    private String register(String username, String email) {
        ResponseEntity<AuthResponse> response = rest.postForEntity("/api/auth/register",
                new RegisterRequest(username, email, "password123", null), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().accessToken();
    }

    private String login(String login, String password) {
        ResponseEntity<AuthResponse> response = rest.postForEntity("/api/auth/login",
                new LoginRequest(login, password), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().accessToken();
    }

    private LookDetailDto fetch(String slug, String token) {
        ResponseEntity<LookDetailDto> response = rest.exchange("/api/looks/" + slug,
                HttpMethod.GET, entity(null, token), LookDetailDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private static <T> HttpEntity<T> entity(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return new HttpEntity<>(body, headers);
    }
}
