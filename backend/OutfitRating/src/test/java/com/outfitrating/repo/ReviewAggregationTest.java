package com.outfitrating.repo;

import static org.assertj.core.api.Assertions.assertThat;

import com.outfitrating.domain.Category;
import com.outfitrating.domain.Look;
import com.outfitrating.domain.LookType;
import com.outfitrating.domain.Review;
import com.outfitrating.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The denormalised averages on a look are only trustworthy if this one query is right, so it gets
 * a test against a real database rather than a mock.
 */
@DataJpaTest
@ActiveProfiles("test")
class ReviewAggregationTest {

    @Autowired
    private ReviewRepository reviews;

    @Autowired
    private LookRepository looks;

    @Autowired
    private UserRepository users;

    private Look look;
    private User author;

    @BeforeEach
    void setUp() {
        author = users.save(user("author"));
        look = looks.save(look(author));
    }

    @Test
    @DisplayName("an unrated look aggregates to zero rather than null counts")
    void emptyAggregate() {
        ScoreAggregate aggregate = reviews.aggregateForLook(look.getId());

        assertThat(aggregate.getRatingsCount()).isZero();
        assertThat(aggregate.getAvgScore()).isNull();
    }

    @Test
    void averagesEveryCriterionAcrossReviews() {
        reviews.save(review(look, users.save(user("a")), (short) 10, (short) 8, (short) 6, (short) 4, (short) 2, (short) 60));
        reviews.save(review(look, users.save(user("b")), (short) 8, (short) 6, (short) 4, (short) 2, (short) 10, (short) 60));
        reviews.flush();

        ScoreAggregate aggregate = reviews.aggregateForLook(look.getId());

        assertThat(aggregate.getRatingsCount()).isEqualTo(2);
        assertThat(aggregate.getAvgStyle()).isEqualTo(9.0);
        assertThat(aggregate.getAvgFit()).isEqualTo(7.0);
        assertThat(aggregate.getAvgColor()).isEqualTo(5.0);
        assertThat(aggregate.getAvgOriginality()).isEqualTo(3.0);
        assertThat(aggregate.getAvgQuality()).isEqualTo(6.0);
        assertThat(aggregate.getAvgScore()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("a bare rating counts towards the score but not towards the written-review total")
    void separatesRatingsFromWrittenReviews() {
        Review bare = review(look, users.save(user("c")), (short) 7, (short) 7, (short) 7, (short) 7, (short) 7, (short) 70);
        reviews.save(bare);

        Review written = review(look, users.save(user("d")), (short) 9, (short) 9, (short) 9, (short) 9, (short) 9, (short) 90);
        written.setBody("Worth the words.");
        reviews.save(written);

        Review blankProse = review(look, users.save(user("e")), (short) 5, (short) 5, (short) 5, (short) 5, (short) 5, (short) 50);
        blankProse.setBody("   ");
        reviews.save(blankProse);
        reviews.flush();

        ScoreAggregate aggregate = reviews.aggregateForLook(look.getId());

        assertThat(aggregate.getRatingsCount()).isEqualTo(3);
        assertThat(aggregate.getReviewsCount()).isEqualTo(1L);
    }

    @Test
    void countsOnlyTheProseReviewsOfOneAuthor() {
        Review written = review(look, author, (short) 6, (short) 6, (short) 6, (short) 6, (short) 6, (short) 60);
        written.setBody("A sentence.");
        reviews.save(written);
        reviews.flush();

        assertThat(reviews.countByAuthorId(author.getId())).isEqualTo(1);
        assertThat(reviews.countProseByAuthor(author.getId())).isEqualTo(1);
    }

    private static User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setPasswordHash("not-a-real-hash");
        user.setDisplayName(username);
        return user;
    }

    private static Look look(User author) {
        Look look = new Look();
        look.setSlug("test-look");
        look.setTitle("Test Look");
        look.setType(LookType.OUTFIT);
        look.setCategory(Category.MINIMAL);
        look.setImageUrl("https://example.test/image.jpg");
        look.setAuthor(author);
        return look;
    }

    private static Review review(Look look, User author, short style, short fit, short color,
                                 short originality, short quality, short score) {
        Review review = new Review();
        review.setLook(look);
        review.setAuthor(author);
        review.setStyle(style);
        review.setFit(fit);
        review.setColor(color);
        review.setOriginality(originality);
        review.setQuality(quality);
        review.setScore(score);
        return review;
    }
}
