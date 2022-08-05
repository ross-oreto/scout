package io.oreto.jpa.dsl.test.entities;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class TestData {
    private static final Random rand = new Random();

    public static String randomString(int size) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        return rand.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(size)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }

    public static Double randomPrice() {
        return BigDecimal.valueOf(rand.nextInt(10000) + rand.nextDouble())
                .setScale(2, RoundingMode.CEILING).doubleValue();
    }

    public static void randomPeople(int people, int orders, int items, EntityManager em) {
        for (int i = 0; i < people; i++) {
            Person person = new Person()
                    .withName(randomString(10))
                    .withAddress(new Address().withLine(randomString(15)));
            for (int j = 0; j < orders; j++) {
                Order order = new Order().withAmount(randomPrice());
                for (int k = 0; k < items; k++) {
                    order.addItem(new Item().withName(randomString(20)));
                }
                person.addOrder(order);
            }
            em.persist(person);
        }
    }

    public static void setupPeople(EntityManager em) {
        em.persist(
                new Person()
                        .withAddress(new Address().withLine("4th Ave Nashville, TN"))
                        .withName("Ross").addNickName("Ross Sauce", "Ross Sea")
                        .addOrder(
                                new Order()
                                        .withAmount(324.33)
                                        .addItem(new Item().withName("knife").addAttribute("type", "forged", "special"))
                        ).addOrder(
                                new Order()
                                        .withAmount(422.19)
                                        .addItem(new Item().withName("Ryzen 5").addAttribute("type", "cpu"))
                        )
        );
        em.persist(
                new Person()
                        .withAddress(new Address().withLine("The Shire"))
                        .withName("Bilbo").addNickName("Barrel Rider", "Riddle Maker").addOrder(
                        new Order()
                                .withAmount(13000000.00)
                                .addItem(new Item().withName("The Ring").addAttribute("type", "forged"))
                                .addItem(new Item().withName("Sting").addAttribute("type", "forged")
                                        .addAttribute("special", "glows"))
                                .addItem(new Item().withName("Arkenstone").addAttribute("alias", "The King's Jewel"))
                )
        );
        em.persist(
                new Person()
                        .withAddress(new Address().withLine("Hogwarts, Gryffindor"))
                        .withName("Harry Potter").addNickName("The chosen one", "The boy who lived").addOrder(
                        new Order()
                                .withAmount(401000000.00)
                                .addItem(new Item().withName("Elder wand")
                                        .addAttribute("core", "tail hair of a Thestral")
                                        .addAttribute("exterior", "elder tree"))
                                .addItem(new Item().withName("Hedwig").addAttribute("species", "Snow Owl"))
                                .addItem(new Item().withName("Sword of Gryffindor")
                                        .addAttribute("special", "destroys horcruxes, willpower and loyality"))
                                .addItem(new Item().withName("eye glasses"))
                )
        );
        em.persist(
                new Person()
                        .withAddress(new Address().withLine("Hogwarts, Gryffindor"))
                        .withName("Ronald Weasley").addNickName("Ron").addOrder(
                                new Order()
                                        .withAmount(324.33)
                                        .addItem(new Item().withName("Scabbers")
                                                .addAttribute("species", "transmutated rat"))
                                        .addItem(new Item().withName("Pigwidgeon").addAttribute("species", "Owl"))
                                        .addItem(new Item().withName("chocolate"))
                                        .addItem(new Item().withName("chocolate"))
                        )
        );
        em.persist(
                new Person()
                        .withAddress(new Address().withLine("Hogwarts, Slug Club"))
                        .withName("Snape").addNickName("The Half Blood Prince").addOrder(
                        new Order()
                                .withAmount(541.00).withShipping(50.55)
                                .addItem(new Item().withName("Ashwinder egg"))
                                .addItem(new Item().withName("Squill buld"))
                                .addItem(new Item().withName("Murtlap tentacle"))
                                .addItem(new Item().withName("Tincture of thyme"))
                ).addOrder(
                       new Order().withAmount(601.44).withShipping(51.39)
                               .addItem(new Item().withName("Occamy eggshell"))
                               .addItem(new Item().withName("Powdered common rue"))
                               .addItem(new Item().withName("glass bottle"))
                               .addItem(new Item().withName("Veritaserum").addAttribute("effect", "Truth serum"))
                        )
        );
        em.persist(
                new Person()
                        .withAddress(new Address().withLine("Hogwarts, Chamber of Secrets"))
                        .withName("Tom Riddle").addNickName("Voldamort").addNickName("The Dark Lord").addOrder(
                        new Order()
                                .withAmount(766.60)
                                .addItem(new Item().withName("Diary"))
                                .addItem(new Item().withName("Ring"))
                                .addItem(new Item().withName("Locket"))
                                .addItem(new Item().withName("Cup"))
                                .addItem(new Item().withName("Diadem"))
                                .addItem(new Item().withName("Snake"))
                )
        );
        em.persist(
                new Person()
                        .withAddress(new Address().withLine("Schenectady, New York"))
                        .withName("Otto Octavius").addNickName("Doc Ock").addNickName("Doctor Octopus").addOrder(
                                new Order().withAmount(30.02).withShipping(100.00).addItem(new Item().withName("Water"))
                        ).addOrder(
                                new Order().withAmount(40.02).withShipping(120.00).addItem(new Item().withName("Salt Water"))
                        )
        );
    }

    public static void setupVehicles(EntityManager em) {
        em.persist(
                new Vehicle()
                        .withMake("Mitsubishi")
                        .withModel("Outlander")
                        .withTire(new Tire().withId(new Tire.TireId("Goodyear", 20)))
        );
        em.persist(
                new Vehicle()
                        .withMake("Mitsubishi")
                        .withModel("Mirage")
                        .withTire(new Tire().withId(new Tire.TireId("Cooper", 13)))
        );
    }
}
