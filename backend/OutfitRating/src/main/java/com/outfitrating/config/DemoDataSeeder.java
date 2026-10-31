package com.outfitrating.config;

import com.outfitrating.domain.Brand;
import com.outfitrating.domain.Category;
import com.outfitrating.domain.Comment;
import com.outfitrating.domain.Favorite;
import com.outfitrating.domain.Follow;
import com.outfitrating.domain.Look;
import com.outfitrating.domain.LookType;
import com.outfitrating.domain.Occasion;
import com.outfitrating.domain.Review;
import com.outfitrating.domain.ReviewLike;
import com.outfitrating.domain.Role;
import com.outfitrating.domain.Season;
import com.outfitrating.domain.Tag;
import com.outfitrating.domain.User;
import com.outfitrating.repo.BrandRepository;
import com.outfitrating.repo.CommentRepository;
import com.outfitrating.repo.FavoriteRepository;
import com.outfitrating.repo.FollowRepository;
import com.outfitrating.repo.LookRepository;
import com.outfitrating.repo.ReviewLikeRepository;
import com.outfitrating.repo.ReviewRepository;
import com.outfitrating.repo.TagRepository;
import com.outfitrating.repo.UserRepository;
import com.outfitrating.service.ReviewService;
import com.outfitrating.service.ScoreCalculator;
import com.outfitrating.service.SlugGenerator;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with a believable catalogue so the app has something to show on first
 * run. Everything here is idempotent by way of the emptiness check — it never touches a database
 * that already holds looks.
 *
 * <p>Review timestamps are deliberately spread across the last seven weeks, otherwise every
 * leaderboard period would return the identical ranking.
 */
@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";
    private static final String UNSPLASH = "https://images.unsplash.com/photo-";
    private static final String IMAGE_PARAMS = "?w=1000&q=80&auto=format&fit=crop";

    private record SeedUser(String username, String displayName, String location, String bio) {
    }

    private record SeedLook(String title, LookType type, Category category, Season season,
                            Occasion occasion, String brand, String photoId, Integer priceCents,
                            String description, String... tags) {
    }

    @Bean
    Seeder demoSeeder(UserRepository users,
                      BrandRepository brands,
                      TagRepository tags,
                      LookRepository looks,
                      ReviewRepository reviews,
                      ReviewLikeRepository reviewLikes,
                      CommentRepository comments,
                      FavoriteRepository favorites,
                      FollowRepository follows,
                      PasswordEncoder passwordEncoder,
                      SlugGenerator slugs,
                      ScoreCalculator scoreCalculator,
                      ReviewService reviewService,
                      EntityManager entityManager) {
        return new Seeder(users, brands, tags, looks, reviews, reviewLikes, comments, favorites, follows,
                passwordEncoder, slugs, scoreCalculator, reviewService, entityManager);
    }

    @Bean
    ApplicationRunner seedDemoData(Seeder seeder) {
        return args -> seeder.run();
    }

    /** Registered as a bean so Spring can wrap {@link #run()} in a transaction. */
    public static class Seeder {

        private final UserRepository users;
        private final BrandRepository brands;
        private final TagRepository tags;
        private final LookRepository looks;
        private final ReviewRepository reviews;
        private final ReviewLikeRepository reviewLikes;
        private final CommentRepository comments;
        private final FavoriteRepository favorites;
        private final FollowRepository follows;
        private final PasswordEncoder passwordEncoder;
        private final SlugGenerator slugs;
        private final ScoreCalculator scoreCalculator;
        private final ReviewService reviewService;
        private final EntityManager entityManager;
        private final Random random = new Random(20240918L);

        Seeder(UserRepository users, BrandRepository brands, TagRepository tags,
               LookRepository looks, ReviewRepository reviews, ReviewLikeRepository reviewLikes,
               CommentRepository comments,
               FavoriteRepository favorites, FollowRepository follows,
               PasswordEncoder passwordEncoder, SlugGenerator slugs,
               ScoreCalculator scoreCalculator, ReviewService reviewService,
               EntityManager entityManager) {
            this.users = users;
            this.brands = brands;
            this.tags = tags;
            this.looks = looks;
            this.reviews = reviews;
            this.reviewLikes = reviewLikes;
            this.comments = comments;
            this.favorites = favorites;
            this.follows = follows;
            this.passwordEncoder = passwordEncoder;
            this.slugs = slugs;
            this.scoreCalculator = scoreCalculator;
            this.reviewService = reviewService;
            this.entityManager = entityManager;
        }

        @Transactional
        public void run() {
            if (looks.count() > 0) {
                log.info("Catalogue already populated ({} looks) — skipping demo seed", looks.count());
                return;
            }
            log.info("Seeding demo catalogue...");

            List<User> members = seedUsers();
            List<Look> catalogue = seedLooks(members);
            seedReviews(members, catalogue);
            seedComments(members, catalogue);
            seedSocialGraph(members, catalogue);

            log.info("Demo catalogue ready: {} members, {} looks, {} reviews. "
                            + "Every demo account uses the password '{}'.",
                    members.size(), catalogue.size(), reviews.count(), DEMO_PASSWORD);
        }

        private List<User> seedUsers() {
            List<SeedUser> seeds = List.of(
                    new SeedUser("mara", "Mara Feld", "Berlin",
                            "Archive hunter. I care about proportion more than logos."),
                    new SeedUser("kenji", "Kenji Aoki", "Tokyo",
                            "Technical fabrics, quiet palettes, obsessive about hems."),
                    new SeedUser("liv", "Liv Sorensen", "Copenhagen",
                            "Minimal by default. If it needs explaining, it is not working."),
                    new SeedUser("theo", "Theo Marchetti", "Milan",
                            "Tailoring first. A jacket that fits beats anything on a runway."),
                    new SeedUser("nadia", "Nadia Rahman", "London",
                            "Colour theory nerd. Neutral outfits are a missed opportunity."),
                    new SeedUser("sasha", "Sasha Petrov", "Saint Petersburg",
                            "Layering in bad weather is the real discipline."),
                    new SeedUser("ines", "Ines Duarte", "Lisbon",
                            "Secondhand only since 2021. Patina is a feature."),
                    new SeedUser("darius", "Darius Cole", "New York",
                            "Sneakers, mostly. I will argue about midsoles."),
                    new SeedUser("yuki", "Yuki Tanaka", "Osaka",
                            "Knitwear, natural dyes, and very slow shopping."),
                    new SeedUser("amina", "Amina Diallo", "Paris",
                            "Silhouette over everything. Volume is a tool, not an accident."));

            List<User> created = new ArrayList<>();
            for (SeedUser seed : seeds) {
                User user = new User();
                user.setUsername(seed.username());
                user.setEmail(seed.username() + "@outfitrating.demo");
                user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
                user.setDisplayName(seed.displayName());
                user.setLocation(seed.location());
                user.setBio(seed.bio());
                user.setAvatarUrl("https://i.pravatar.cc/240?u=" + seed.username());
                user.setRole("mara".equals(seed.username()) ? Role.ADMIN
                        : "liv".equals(seed.username()) ? Role.CURATOR : Role.USER);
                created.add(users.save(user));
            }
            return created;
        }

        private List<Look> seedLooks(List<User> members) {
            List<SeedLook> seeds = catalogue();
            List<Look> created = new ArrayList<>();
            for (int i = 0; i < seeds.size(); i++) {
                SeedLook seed = seeds.get(i);
                Look look = new Look();
                look.setTitle(seed.title());
                look.setSlug(slugs.uniqueSlug(seed.title(), looks::existsBySlug));
                look.setType(seed.type());
                look.setCategory(seed.category());
                look.setSeason(seed.season());
                look.setOccasion(seed.occasion());
                look.setDescription(seed.description());
                look.setImageUrl(UNSPLASH + seed.photoId() + IMAGE_PARAMS);
                look.setBrand(seed.brand() == null ? null : brandNamed(seed.brand()));
                look.setAuthor(members.get(i % members.size()));
                look.setPriceCents(seed.priceCents());
                look.setCurrency(seed.priceCents() == null ? null : "EUR");
                look.setReleaseDate(LocalDate.now().minusDays(random.nextInt(280)));
                look.setFeatured(i % 7 == 0);
                look.setTags(tagsNamed(seed.tags()));
                created.add(looks.save(look));
            }
            return created;
        }

        private void seedReviews(List<User> members, List<Look> catalogue) {
            for (Look look : catalogue) {
                // A quality gradient so leaderboards are not a flat field of 70s.
                int centre = 55 + random.nextInt(38);
                int reviewCount = 2 + random.nextInt(9);

                List<User> reviewers = new ArrayList<>(members);
                reviewers.removeIf(member -> member.getId().equals(look.getAuthor().getId()));
                java.util.Collections.shuffle(reviewers, random);

                for (int i = 0; i < Math.min(reviewCount, reviewers.size()); i++) {
                    User reviewer = reviewers.get(i);
                    Review review = new Review();
                    review.setLook(look);
                    review.setAuthor(reviewer);
                    review.setStyle(criterion(centre));
                    review.setFit(criterion(centre));
                    review.setColor(criterion(centre));
                    review.setOriginality(criterion(centre));
                    review.setQuality(criterion(centre));
                    review.setScore(scoreCalculator.compose(review.getStyle(), review.getFit(),
                            review.getColor(), review.getOriginality(), review.getQuality()));
                    if (random.nextDouble() < 0.55) {
                        review.setTitle(reviewTitle(review.getScore()));
                        review.setBody(reviewBody(look, review.getScore()));
                    }
                    reviews.save(review);
                    backdate(review);
                    likeReview(review, members);
                }
                reviews.flush();
                reviewService.recomputeAggregates(look);
            }
        }

        /**
         * Helpful votes are seeded as real rows rather than a bare counter. Writing only the
         * denormalised count would look right until the first genuine like recomputed it from the
         * table and wiped the fabricated number out.
         */
        private void likeReview(Review review, List<User> members) {
            int likes = 0;
            for (User member : members) {
                if (member.getId().equals(review.getAuthor().getId()) || random.nextDouble() > 0.3) {
                    continue;
                }
                ReviewLike like = new ReviewLike();
                like.setReview(review);
                like.setUser(member);
                reviewLikes.save(like);
                likes++;
            }
            review.setLikesCount(likes);
        }

        private void seedComments(List<User> members, List<Look> catalogue) {
            String[] openers = {
                    "The shoe choice is doing a lot of work here.",
                    "Would this hold up in actual rain, though?",
                    "That colour combination should not work and yet.",
                    "Proportions are spot on. The length of the coat especially.",
                    "I own this exact piece and it fits nothing like that on me.",
                    "Underrated. This deserves more eyes.",
                    "Feels derivative of last season, but executed well.",
                    "The texture contrast is the whole outfit.",
            };
            String[] replies = {
                    "Agreed, though I would size down.",
                    "Fair, but that is the point of the silhouette.",
                    "Same experience here.",
                    "Hard disagree, the colour is the weak link.",
                    "This comment made me look again.",
            };

            for (Look look : catalogue) {
                if (random.nextDouble() > 0.6) {
                    continue;
                }
                int count = 1 + random.nextInt(3);
                for (int i = 0; i < count; i++) {
                    Comment root = new Comment();
                    root.setLook(look);
                    root.setAuthor(members.get(random.nextInt(members.size())));
                    root.setBody(openers[random.nextInt(openers.length)]);
                    comments.save(root);

                    if (random.nextDouble() < 0.45) {
                        Comment reply = new Comment();
                        reply.setLook(look);
                        reply.setAuthor(members.get(random.nextInt(members.size())));
                        reply.setParent(root);
                        reply.setBody(replies[random.nextInt(replies.length)]);
                        comments.save(reply);
                    }
                }
            }
        }

        private void seedSocialGraph(List<User> members, List<Look> catalogue) {
            for (User member : members) {
                for (User other : members) {
                    if (!member.getId().equals(other.getId()) && random.nextDouble() < 0.35) {
                        Follow follow = new Follow();
                        follow.setFollower(member);
                        follow.setFollowing(other);
                        follows.save(follow);
                    }
                }
                for (Look look : catalogue) {
                    if (random.nextDouble() < 0.08
                            && !look.getAuthor().getId().equals(member.getId())) {
                        Favorite favorite = new Favorite();
                        favorite.setUser(member);
                        favorite.setLook(look);
                        favorites.save(favorite);
                    }
                }
            }
        }

        /** Spreads a review across the last seven weeks so period charts actually differ. */
        private void backdate(Review review) {
            Instant when = Instant.now()
                    .minus(random.nextInt(49), ChronoUnit.DAYS)
                    .minus(random.nextInt(24), ChronoUnit.HOURS);
            entityManager.createNativeQuery(
                            "update reviews set created_at = :when, updated_at = :when where id = :id")
                    .setParameter("when", when)
                    .setParameter("id", review.getId())
                    .executeUpdate();
        }

        private short criterion(int centre) {
            double base = centre / 10.0 + (random.nextGaussian() * 1.15);
            return (short) Math.max(ScoreCalculator.MIN_CRITERION,
                    Math.min(ScoreCalculator.MAX_CRITERION, Math.round(base)));
        }

        private String reviewTitle(short score) {
            if (score >= 88) {
                return "This is the bar now";
            }
            if (score >= 75) {
                return "Confident and well judged";
            }
            if (score >= 60) {
                return "Good bones, one weak link";
            }
            if (score >= 45) {
                return "Nearly, but the proportions fight each other";
            }
            return "Ambitious, does not land";
        }

        private String reviewBody(Look look, short score) {
            String subject = look.getType() == LookType.OUTFIT ? "the outfit" : "the piece";
            if (score >= 85) {
                return "Everything in " + subject + " is pulling in the same direction. The "
                        + "silhouette reads clearly from a distance, the palette is disciplined, "
                        + "and nothing is there purely to be noticed. The kind of thing that looks "
                        + "obvious only after somebody else has done it.";
            }
            if (score >= 70) {
                return "Strong overall. The proportions are handled well and the material choice "
                        + "earns its place. My only reservation is that it plays safe in one or two "
                        + "spots where a sharper decision would have lifted the whole thing.";
            }
            if (score >= 55) {
                return "There is a good idea underneath, and parts of it work — but the balance is "
                        + "off. One element is doing too much and the rest is compensating. Worth "
                        + "revisiting with a simpler foundation.";
            }
            return "The intent is clear and I respect the ambition, but the execution does not get "
                    + "there. The volumes compete rather than complement, and the finish lets it "
                    + "down under any real scrutiny.";
        }

        private Brand brandNamed(String name) {
            return brands.findByNameIgnoreCase(name).orElseGet(() -> {
                Brand brand = new Brand();
                brand.setName(name);
                brand.setSlug(slugs.uniqueSlug(name, slug -> brands.findBySlug(slug).isPresent()));
                brand.setCountry(brandCountry(name));
                brand.setDescription(name + " — one of the labels the community tracks most closely.");
                return brands.save(brand);
            });
        }

        private String brandCountry(String name) {
            return switch (name) {
                case "Acne Studios", "Our Legacy" -> "Sweden";
                case "Comme des Garcons", "Sacai", "Issey Miyake", "Visvim" -> "Japan";
                case "Lemaire", "A.P.C." -> "France";
                case "Stone Island", "Prada" -> "Italy";
                case "Carhartt WIP", "New Balance", "Nike" -> "United States";
                case "Margiela" -> "Belgium";
                default -> null;
            };
        }

        private Set<Tag> tagsNamed(String... names) {
            Set<Tag> resolved = new LinkedHashSet<>();
            for (String raw : names) {
                String name = raw.toLowerCase(Locale.ROOT);
                resolved.add(tags.findByNameIgnoreCase(name).orElseGet(() -> {
                    Tag tag = new Tag();
                    tag.setName(name);
                    tag.setSlug(slugs.uniqueSlug(name, slug -> tags.findBySlug(slug).isPresent()));
                    return tags.save(tag);
                }));
            }
            return resolved;
        }

        private List<SeedLook> catalogue() {
            return List.of(
                    new SeedLook("Blackout Layers, Kreuzberg", LookType.OUTFIT, Category.STREETWEAR,
                            Season.AUTUMN, Occasion.EVERYDAY, "Acne Studios", "1660486044177-45cd45bb5e99",
                            null, "Head-to-toe black with three different textures doing the work "
                            + "that colour usually does. Worn on a grey Tuesday and it held up.",
                            "monochrome", "layering", "berlin"),
                    new SeedLook("Grain and Grey", LookType.OUTFIT, Category.STREETWEAR,
                            Season.WINTER, Occasion.EVERYDAY, "Carhartt WIP", "1532332248682-206cc786359f",
                            null, "Workwear shell over a heavy hood. Nothing precious, everything "
                            + "functional, and it photographs better than it has any right to.",
                            "workwear", "monochrome"),
                    new SeedLook("Fog Hoodie Study", LookType.OUTFIT, Category.STREETWEAR,
                            Season.AUTUMN, Occasion.EVERYDAY, null, "1649114383220-c4f0f0dbafbe",
                            null, "One grey hoodie, one wide trouser, and a lot of attention paid "
                            + "to where the hem lands. That is the entire outfit.", "minimal", "grey"),
                    new SeedLook("Runway Overload", LookType.OUTFIT, Category.AVANT_GARDE,
                            Season.SPRING, Occasion.RUNWAY, "Comme des Garcons", "1624353656309-8be1a6c457be",
                            null, "Print on print on print. It should be a disaster and it is "
                            + "somehow coherent — the cut is doing the heavy lifting.",
                            "print", "maximalist", "runway"),
                    new SeedLook("Soft Shell Commute", LookType.OUTFIT, Category.TECHWEAR,
                            Season.AUTUMN, Occasion.TRAVEL, "Stone Island", "1647218947427-d783309440d2",
                            null, "Built for a forty-minute walk in weather that cannot decide. "
                            + "Muted enough to wear into an office afterwards.", "techwear", "grey"),
                    new SeedLook("Black Suit, No Tie", LookType.OUTFIT, Category.FORMAL,
                            Season.ALL_SEASON, Occasion.EVENING, "Prada", "1536243298747-ea8874136d64",
                            null, "The suit that works when the invitation does not specify. "
                            + "Open collar keeps it from reading as a uniform.", "tailoring", "monochrome"),
                    new SeedLook("Cargo Discipline", LookType.OUTFIT, Category.STREETWEAR,
                            Season.SUMMER, Occasion.EVERYDAY, null, "1552902865-b72c031ac5ea",
                            null, "Wide cargo with a cropped top — the volume is deliberate and the "
                            + "waist is the only fitted point. Restraint in a loud category.",
                            "cargo", "volume"),
                    new SeedLook("Red Midsole Statement", LookType.OUTFIT, Category.STREETWEAR,
                            Season.SPRING, Occasion.EVERYDAY, "Nike", "1628030328071-538b251a4455",
                            null, "Built the entire fit around one pair of shoes, which is usually "
                            + "a mistake. Here the rest stays quiet enough to let it work.",
                            "sneakers", "red"),
                    new SeedLook("Neon Against Concrete", LookType.OUTFIT, Category.STREETWEAR,
                            Season.SPRING, Occasion.PARTY, null, "1547069553-12f23c839aaa",
                            null, "A single saturated green against a grey city. The confidence "
                            + "is the point; anything subtler would have disappeared.",
                            "colour", "statement"),
                    new SeedLook("Flatlay: Weekend Minimum", LookType.OUTFIT, Category.MINIMAL,
                            Season.SUMMER, Occasion.EVERYDAY, "Our Legacy", "1525507119028-ed4c629a60a3",
                            null, "Four pieces, one bag, two days away. The test of a wardrobe is "
                            + "what survives the edit.", "minimal", "capsule"),
                    new SeedLook("Sand and Slate", LookType.OUTFIT, Category.MINIMAL,
                            Season.AUTUMN, Occasion.OFFICE, "Lemaire", "1516762689617-e1cffcef479d",
                            null, "Two neutrals, one texture break. Reads expensive without a "
                            + "single visible label.", "neutral", "minimal", "capsule"),
                    new SeedLook("White Shirt Doctrine", LookType.OUTFIT, Category.MINIMAL,
                            Season.ALL_SEASON, Occasion.OFFICE, "A.P.C.", "1467043237213-65f2da53396f",
                            null, "There is no more tested idea in clothing. The whole outfit lives "
                            + "or dies on the collar and the cuff.", "minimal", "classic"),
                    new SeedLook("Rust Knit, Raw Denim", LookType.OUTFIT, Category.KNITWEAR,
                            Season.AUTUMN, Occasion.EVERYDAY, null, "1556905055-8f358a7a47b2",
                            null, "Warm knit against cold indigo. The pairing is old but the "
                            + "proportion here is current.", "knitwear", "denim", "autumn"),
                    new SeedLook("Indigo and White Soles", LookType.OUTFIT, Category.DENIM,
                            Season.SPRING, Occasion.EVERYDAY, null, "1544441893-675973e31985",
                            null, "Denim on denim only works when the washes disagree enough. "
                            + "These do, by about two shades.", "denim", "sneakers"),
                    new SeedLook("Two People, One Palette", LookType.OUTFIT, Category.DENIM,
                            Season.SUMMER, Occasion.EVERYDAY, null, "1507297448044-a99b358cd06e",
                            null, "Coordinated without matching, which is a harder trick than it "
                            + "looks. Note how the shoes break it up.", "denim", "summer"),
                    new SeedLook("The Rail Edit", LookType.OUTFIT, Category.MINIMAL,
                            Season.ALL_SEASON, Occasion.OFFICE, "Our Legacy", "1524282745852-a463fa495a7f",
                            null, "Everything on the rail shares a palette, which is why getting "
                            + "dressed in the morning takes ninety seconds.", "capsule", "minimal"),
                    new SeedLook("Midnight Tailoring", LookType.OUTFIT, Category.FORMAL,
                            Season.WINTER, Occasion.EVENING, "Margiela", "1618886614638-80e3c103d31a",
                            null, "A black suit cut close enough to look intentional rather than "
                            + "borrowed. The shoulder is the whole argument.", "tailoring", "evening"),
                    new SeedLook("Sunglasses at the Ceremony", LookType.OUTFIT, Category.FORMAL,
                            Season.SUMMER, Occasion.EVENING, null, "1617127365659-c47fa864d8bc",
                            null, "Slightly too much, entirely on purpose. Works because the "
                            + "tailoring underneath is genuinely good.", "tailoring", "statement"),
                    new SeedLook("Cobalt Three-Piece", LookType.OUTFIT, Category.FORMAL,
                            Season.SPRING, Occasion.OFFICE, null, "1623880840102-7df0a9f3545b",
                            null, "Colour in tailoring is a risk. This one manages it by keeping "
                            + "everything else completely plain.", "tailoring", "colour"),
                    new SeedLook("Field Grey Suiting", LookType.OUTFIT, Category.FORMAL,
                            Season.AUTUMN, Occasion.EVENING, null, "1548454782-15b189d129ab",
                            null, "Soft-shouldered, slightly rumpled, photographed outdoors where "
                            + "tailoring usually is not. Better for it.", "tailoring", "autumn"),
                    new SeedLook("Camel Coat, Long Line", LookType.OUTFIT, Category.OUTERWEAR,
                            Season.WINTER, Occasion.OFFICE, "Lemaire", "1539533113208-f6df8cc8b543",
                            null, "The coat that makes everything underneath irrelevant. Length "
                            + "below the knee is what separates it from a blazer.",
                            "outerwear", "camel", "winter"),
                    new SeedLook("Back of the Camel", LookType.OUTFIT, Category.OUTERWEAR,
                            Season.WINTER, Occasion.EVERYDAY, null, "1539533018447-63fcce2678e3",
                            null, "Half of a coat's job is how it looks walking away. This one "
                            + "passes.", "outerwear", "camel"),
                    new SeedLook("Street Style, Late October", LookType.OUTFIT, Category.STREETWEAR,
                            Season.AUTUMN, Occasion.EVERYDAY, null, "1594748504715-2e715b1034bf",
                            null, "Caught between seasons and solving it with layers rather than "
                            + "a heavier coat. The right answer.", "layering", "autumn"),
                    new SeedLook("White Coat in Snow", LookType.OUTFIT, Category.OUTERWEAR,
                            Season.WINTER, Occasion.EVENING, null, "1611747582190-feff22542ba1",
                            null, "Impractical and completely worth it. Tonal white against snow "
                            + "is a harder photograph than it looks.", "outerwear", "winter", "white"),
                    new SeedLook("Trench and a Red Line", LookType.OUTFIT, Category.OUTERWEAR,
                            Season.AUTUMN, Occasion.OFFICE, "Prada", "1550872199-63f4382fe925",
                            null, "Classic trench, one deliberate flash of red. That single "
                            + "decision is what stops it being a costume.", "outerwear", "red"),
                    new SeedLook("Denim Jacket, Sun-Faded", LookType.ITEM, Category.DENIM,
                            Season.SPRING, null, "Levi's", "1611312449408-fcece27cdbb7", 18900,
                            "Eight years of wear and the fade map is unrepeatable. This is what "
                            + "people mean when they say denim earns its character.", "denim", "vintage"),
                    new SeedLook("Trucker Jacket Over Knit", LookType.OUTFIT, Category.VINTAGE,
                            Season.SPRING, Occasion.EVERYDAY, "Levi's", "1555583743-991174c11425",
                            null, "The most copied jacket in existence, still working. Layered "
                            + "over knit rather than under a coat.", "denim", "vintage"),
                    new SeedLook("Second-Hand Blue", LookType.OUTFIT, Category.VINTAGE,
                            Season.SUMMER, Occasion.EVERYDAY, null, "1580911035415-73036934b4e0",
                            null, "Entirely secondhand, assembled over two years. Nothing here "
                            + "was bought to complete an outfit.", "vintage", "secondhand", "denim"),
                    new SeedLook("Air Force Pastel", LookType.ITEM, Category.FOOTWEAR,
                            Season.SPRING, null, "Nike", "1595950653106-6c9ebd614d3a", 12900,
                            "A silhouette that has survived forty years of trend cycles by "
                            + "refusing to change. The pastel is the only concession.",
                            "sneakers", "pastel"),
                    new SeedLook("All-White Runner", LookType.ITEM, Category.FOOTWEAR,
                            Season.SUMMER, null, "Nike", "1600269452121-4f2416e55c28", 13900,
                            "Clean for about four days, then it becomes a different shoe. Whether "
                            + "that is a flaw depends entirely on you.", "sneakers", "white"),
                    new SeedLook("Floral Panel Sneaker", LookType.ITEM, Category.FOOTWEAR,
                            Season.SPRING, null, null, "1560769629-975ec94e6a86", 15900,
                            "Print on a sneaker usually ages badly. The restraint in the panel "
                            + "placement is why this one might not.", "sneakers", "print"),
                    new SeedLook("Court Classic, Off-White", LookType.ITEM, Category.FOOTWEAR,
                            Season.ALL_SEASON, null, "New Balance", "1608231387042-66d1773070a5", 10900,
                            "Boring in the best sense. Goes with everything, argues with nothing, "
                            + "lasts two years of daily wear.", "sneakers", "minimal"),
                    new SeedLook("Yellow Ground AF1", LookType.ITEM, Category.FOOTWEAR,
                            Season.SUMMER, null, "Nike", "1549298916-b41d501d3772", 12900,
                            "Shot against yellow because the shoe itself gives you nothing to "
                            + "photograph. That is the point of it.", "sneakers", "classic"),
                    new SeedLook("Chicago Colourway", LookType.ITEM, Category.FOOTWEAR,
                            Season.ALL_SEASON, null, "Nike", "1552346154-21d32810aba3", 21900,
                            "The colourway everything else is measured against. Price reflects "
                            + "the story more than the shoe.", "sneakers", "red", "archive"),
                    new SeedLook("Canvas High Top", LookType.ITEM, Category.FOOTWEAR,
                            Season.SUMMER, null, null, "1512374382149-233c42b6a83b", 6900,
                            "Structurally unremarkable and completely indispensable. The only "
                            + "shoe on this list under seventy euro.", "sneakers", "classic"),
                    new SeedLook("Mesh Runner, Grey", LookType.ITEM, Category.FOOTWEAR,
                            Season.SUMMER, null, "Nike", "1595341888016-a392ef81b7de", 14900,
                            "Nineties running geometry, rebuilt. Divisive, which is usually a "
                            + "sign the design committed to something.", "sneakers", "archive"),
                    new SeedLook("Cable Knit, Undyed", LookType.ITEM, Category.KNITWEAR,
                            Season.WINTER, null, "Visvim", "1574201635302-388dd92a4c3f", 34900,
                            "Undyed wool, heavy gauge, and a collar that holds its shape. "
                            + "Expensive in a way that is at least legible.", "knitwear", "natural"),
                    new SeedLook("Hanger Knit, Oatmeal", LookType.ITEM, Category.KNITWEAR,
                            Season.AUTUMN, null, "Our Legacy", "1631541909061-71e349d1f203", 19900,
                            "The colour is the whole purchase. Oatmeal sits between every other "
                            + "neutral without competing.", "knitwear", "neutral"),
                    new SeedLook("Stacked Wool", LookType.ITEM, Category.KNITWEAR,
                            Season.WINTER, null, null, "1601379327928-bedfaf9da2d0", 12900,
                            "Three weights of the same sweater. The heaviest is the only one that "
                            + "survives a real winter.", "knitwear", "winter"),
                    new SeedLook("Forest Green Oversize", LookType.OUTFIT, Category.KNITWEAR,
                            Season.AUTUMN, Occasion.EVERYDAY, null, "1580331451062-99ff652288d7",
                            null, "Volume up top, nothing complicated below. Green this deep is "
                            + "effectively a neutral.", "knitwear", "green", "volume"),
                    new SeedLook("Denim Jacket, Indoor Light", LookType.OUTFIT, Category.DENIM,
                            Season.SPRING, Occasion.EVERYDAY, null, "1556041068-5874261f23e5",
                            null, "Oversized through the body, cropped at the hip. The proportion "
                            + "is what makes an ordinary jacket read as current.", "denim", "volume"),
                    new SeedLook("Denim and a Shoulder Bag", LookType.OUTFIT, Category.CASUAL,
                            Season.SPRING, Occasion.TRAVEL, null, "1601036559620-3a83dfdead09",
                            null, "Built for a day of walking. The bag is the only styling "
                            + "decision and it is the right one.", "denim", "travel"),
                    new SeedLook("Workwear Blue", LookType.OUTFIT, Category.STREETWEAR,
                            Season.AUTUMN, Occasion.EVERYDAY, "Carhartt WIP", "1634291934402-7968f44a2939",
                            null, "Chore coat in the original weight. Reads as heritage now, "
                            + "which was never the intention.", "workwear", "denim"),
                    new SeedLook("Desk of Small Decisions", LookType.ITEM, Category.ACCESSORIES,
                            Season.ALL_SEASON, null, null, "1492707892479-7bc8d5a4ee93", 8900,
                            "Sunglasses, a watch, a wallet. The three things that quietly decide "
                            + "whether an outfit is finished.", "accessories", "flatlay"),
                    new SeedLook("Structured Black Bag", LookType.ITEM, Category.ACCESSORIES,
                            Season.ALL_SEASON, null, "Prada", "1559563458-527698bf5295", 89000,
                            "Hardware-free and rigid enough to stand on its own. The price is "
                            + "indefensible and it will still outlast three cheaper bags.",
                            "accessories", "bag"),
                    new SeedLook("Dove Grey Shoulder Bag", LookType.ITEM, Category.ACCESSORIES,
                            Season.SPRING, null, null, "1506152983158-b4a74a01c721", 24900,
                            "Grey leather is underrated — it takes wear better than black and "
                            + "goes with more than tan.", "accessories", "bag", "neutral"),
                    new SeedLook("Teal Top Handle", LookType.ITEM, Category.ACCESSORIES,
                            Season.SUMMER, null, null, "1523779105320-d1cd346ff52b", 31900,
                            "One saturated object in an otherwise neutral wardrobe. That is the "
                            + "entire strategy and it works.", "accessories", "bag", "colour"),
                    new SeedLook("Pleats and Movement", LookType.OUTFIT, Category.AVANT_GARDE,
                            Season.SPRING, Occasion.RUNWAY, "Issey Miyake", "1621341103818-01dada8c6ef8",
                            null, "The garment only exists properly in motion, which makes it "
                            + "almost impossible to photograph fairly.", "pleats", "runway", "volume"),
                    new SeedLook("Hybrid Shell, Split Panel", LookType.OUTFIT, Category.TECHWEAR,
                            Season.WINTER, Occasion.TRAVEL, "Sacai", "1542219550-2da790bf52e9",
                            null, "Two garments spliced into one. Either half would be ordinary; "
                            + "the seam is the design.", "techwear", "hybrid"),
                    new SeedLook("Gallery Opening Grey", LookType.OUTFIT, Category.MINIMAL,
                            Season.AUTUMN, Occasion.EVENING, "Margiela", "1603394151492-5e9b974b090b",
                            null, "Dressed enough for the room, plain enough to be forgotten in "
                            + "photographs. Sometimes that is the brief.", "minimal", "evening"));
        }
    }
}
