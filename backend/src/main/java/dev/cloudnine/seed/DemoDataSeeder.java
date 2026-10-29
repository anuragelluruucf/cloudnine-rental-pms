package dev.cloudnine.seed;

import dev.cloudnine.domain.ChannelType;
import dev.cloudnine.domain.Role;
import dev.cloudnine.domain.UserAccount;
import dev.cloudnine.repo.UserAccountRepository;
import dev.cloudnine.service.ChannelService;
import dev.cloudnine.service.PropertyService;
import dev.cloudnine.service.ReservationService;
import dev.cloudnine.web.Dtos.ChannelRequest;
import dev.cloudnine.web.Dtos.PropertyRequest;
import dev.cloudnine.web.Dtos.ReservationRequest;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "cloudnine.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    static final String DEMO_PASSWORD = "cloudnine-demo";

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PropertyService properties;
    private final ReservationService reservations;
    private final ChannelService channels;

    public DemoDataSeeder(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            PropertyService properties,
            ReservationService reservations,
            ChannelService channels
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.reservations = reservations;
        this.channels = channels;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByEmailIgnoreCase("mara@cloudnine.test")) {
            return;
        }
        UserAccount mara = user("Mara Ellison", "mara@cloudnine.test", Role.MANAGER);
        UserAccount admin = user("Avery Quinn", "admin@cloudnine.test", Role.ADMIN);
        users.save(admin);

        LocalDate today = LocalDate.now();
        var cedar = properties.create(mara, new PropertyRequest(
                "Cedar & Pine Cabin",
                "18 Birch Lane, Bend, Oregon",
                "A two-bedroom cabin tucked into ponderosa pines, with a wood stove and a deck that faces the ridge.",
                "Quiet hours are 10pm to 8am. No smoking anywhere on the property. Shoes off inside. Two dogs are fine if they are noted on the booking.",
                "Wood stove, washer and dryer, pack-and-play, board games, gas grill on the back deck, two parking spots on the gravel pad.",
                "Check-in is at 4:00 pm. The lockbox is on the left porch post and the code is 4419. Park on the gravel pad and please don't block the neighbor's hydrant.",
                "Check-out is at 10:00 am. Start the dishwasher, take the trash to the bear bin at the end of the drive, and lock the door behind you.",
                "CedarPine-Guest",
                "pinecone-1842",
                "The grill is on the back deck and propane is supplied. Trash pickup is Thursday morning. The hot tub rules: no glass, and cover it when you leave. Extra blankets are in the hall closet."
        ));
        channels.create(mara, cedar.id(), new ChannelRequest(ChannelType.AIRBNB, null));
        channels.create(mara, cedar.id(), new ChannelRequest(ChannelType.VRBO, null));
        reservations.create(mara, cedar.id(), new ReservationRequest(
                "Jonah Adler", "jonah@example.test", today.plusDays(2), today.plusDays(6),
                ChannelType.DIRECT, "Anniversary trip. Arriving around 5pm.", false));
        reservations.create(mara, cedar.id(), new ReservationRequest(
                "Priya Raman", "priya@example.test", LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 21),
                ChannelType.AIRBNB, "Seeded stay that overlaps the sample Airbnb feed.", false));

        var studio = properties.create(mara, new PropertyRequest(
                "Saltglass Studio",
                "4 Wharf Street, Rockport, Maine",
                "A bright one-room studio above a closed net loft, two blocks from the harbor.",
                "No shoes upstairs. No parties. Quiet after 9pm because the walls are old.",
                "Kitchenette, queen bed, beach chairs in the closet, coffee press, Bluetooth speaker.",
                "Check-in is at 3:00 pm. Pick up the key from Moth & Fig cafe next door and give them the name on the reservation.",
                "Check-out is at 11:00 am. Leave the key on the kitchen table.",
                "Saltglass-Guest",
                "tidepool-77",
                "Street parking is free after 6pm. The harbor path starts at the end of the block. The cafe downstairs opens at 7am."
        ));
        channels.create(mara, studio.id(), new ChannelRequest(ChannelType.BOOKING, null));
        reservations.create(mara, studio.id(), new ReservationRequest(
                "Elena Voss", "elena@example.test", today.plusDays(1), today.plusDays(4),
                ChannelType.DIRECT, null, false));
        reservations.create(mara, studio.id(), new ReservationRequest(
                null, null, today.plusDays(10), today.plusDays(12),
                ChannelType.DIRECT, "Owner stay. Not bookable.", true));
    }

    private UserAccount user(String name, String email, Role role) {
        UserAccount user = new UserAccount();
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        return users.save(user);
    }
}
