package com.kindererp.tools;

import com.kindererp.KinderErpApplication;
import com.kindererp.config.AppPaths;
import com.kindererp.model.PaymentMethod;
import com.kindererp.model.SchoolSettings;
import com.kindererp.service.*;
import com.kindererp.service.dto.SetupRequest;
import com.kindererp.util.*;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Developer tool (not shipped): opens every screen with demo data, in French and in Arabic, and saves
 * a PNG of each into the folder given as first argument. Used to check layouts and right-to-left.
 */
public final class ScreenshotTour {

    private static final String[][] MODULES = {
            {"students", "showStudents"}, {"parents", "showParents"}, {"tuition", "showTuition"},
            {"attendance", "showAttendance"}, {"staff", "showStaff"}, {"staff_attendance", "showStaffAttendance"},
            {"payroll", "showPayroll"}, {"settings", "showSettings"}};

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "target/screenshots");
        Path home = Files.createTempDirectory("kindererp-tour");
        System.setProperty("kindererp.home", home.toString());
        AppPaths.init();
        Files.createDirectories(out);

        ConfigurableApplicationContext context = new SpringApplicationBuilder(KinderErpApplication.class).headless(false).run();
        ViewLoader.setContext(context);
        seed(context);

        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        started.await();
        fx(() -> {
            for (String font : new String[]{"/fonts/Tajawal-Regular.ttf", "/fonts/Tajawal-Bold.ttf"}) {
                javafx.scene.text.Font.loadFont(ScreenshotTour.class.getResourceAsStream(font), 13);
            }
        });

        for (I18n.Language language : I18n.Language.values()) {
            fx(() -> {
                I18n.setLanguage(language);
                AppState.setSettings(context.getBean(SchoolSettingsService.class).get());
                AppState.setCurrentUser(context.getBean(UserService.class).authenticate("admin", "admin123").orElseThrow());
                AppState.setCurrentSchoolYear(context.getBean(SchoolYearService.class).findCurrent());
            });
            String lang = language.code();
            shoot(out, lang + "-login", () -> ViewLoader.load("login").root(), 460, 620);
            shoot(out, lang + "-setup", () -> ViewLoader.load("setup").root(), 860, 760);
            shoot(out, lang + "-license", () -> ViewLoader.load("license").root(), 640, 660);
            for (String[] module : MODULES) {
                shoot(out, lang + "-" + module[0], () -> {
                    ViewLoader.View<Object> main = ViewLoader.load("main");
                    invoke(main.controller(), module[1]);
                    return main.root();
                }, 1280, 800);
            }
            shoot(out, lang + "-student_form", () -> {
                ViewLoader.View<com.kindererp.controller.StudentFormController> v = ViewLoader.load("student_form");
                v.controller().setStudent(context.getBean(StudentService.class).findAll().get(0));
                return v.root();
            }, 520, 470);
            shoot(out, lang + "-staff_form", () -> {
                ViewLoader.View<com.kindererp.controller.StaffFormController> v = ViewLoader.load("staff_form");
                v.controller().setMember(StaffKind.TEACHER, context.getBean(StaffService.class).findAll(StaffKind.TEACHER).get(0));
                return v.root();
            }, 560, 560);
            shoot(out, lang + "-payment_dialog", () -> {
                ViewLoader.View<com.kindererp.controller.PaymentDialogController> v = ViewLoader.load("payment_dialog");
                var parent = context.getBean(ParentService.class).findAll().get(1);
                var month = context.getBean(SchoolYearService.class).months(AppState.currentSchoolYear().getId()).get(0);
                v.controller().setData(parent, month);
                return v.root();
            }, 480, 520);
        }
        Platform.exit();
        context.close();
        try (Stream<Path> files = Files.walk(home)) {
            files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
        System.out.println("Screenshots written to " + out.toAbsolutePath());
        System.exit(0);
    }

    private static void seed(ConfigurableApplicationContext context) {
        SchoolSettings settings = new SchoolSettings();
        settings.setNameFr("Jardin d'enfants Exemple");
        settings.setNameAr("روضة المثال");
        settings.setAddress("1 rue de l'Exemple, Ville");
        settings.setPhone("70 000 000");
        settings.setMonthlyFee(new BigDecimal("120"));
        settings.setDiscountTwoChildren(new BigDecimal("10"));
        settings.setDiscountThreePlusChildren(new BigDecimal("25"));
        String year = SchoolYearService.suggestedName(settings.getYearStartMonth());
        context.getBean(SetupService.class).completeSetup(new SetupRequest(settings, "admin", "Admin Exemple", "admin123", year, true));
        var months = context.getBean(SchoolYearService.class).months(context.getBean(SchoolYearService.class).findCurrent().getId());
        var parent = context.getBean(ParentService.class).findAll().get(0);
        context.getBean(PaymentService.class).registerPayment(parent.getId(), months.get(0).getId(), PaymentMethod.CASH, null);
    }

    private interface ViewFactory {
        Parent create() throws Exception;
    }

    private static void shoot(Path out, String name, ViewFactory factory, int width, int height) throws Exception {
        Stage[] stage = new Stage[1];
        fx(() -> {
            stage[0] = new Stage();
            Parent root = factory.create();
            Scene scene = ViewLoader.scene(root);
            stage[0].setScene(scene);
            stage[0].setWidth(width);
            stage[0].setHeight(height);
            stage[0].show();
        });
        Thread.sleep(1800);
        fx(() -> {
            WritableImage image = stage[0].getScene().snapshot(null);
            ImageIO.write(toBuffered(image), "png", out.resolve(name + ".png").toFile());
            stage[0].close();
        });
    }

    private static BufferedImage toBuffered(WritableImage image) {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        return buffered;
    }

    private static void invoke(Object target, String method) throws Exception {
        Method m = target.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(target);
    }

    private interface FxAction {
        void run() throws Exception;
    }

    private static void fx(FxAction action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Exception[] error = new Exception[1];
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Exception e) {
                error[0] = e;
            } finally {
                done.countDown();
            }
        });
        if (!done.await(30, TimeUnit.SECONDS)) {
            Thread.getAllStackTraces().forEach((t, stack) -> {
                if (t.getName().contains("JavaFX Application")) {
                    System.out.println("FX THREAD:");
                    for (StackTraceElement e : stack) System.out.println("    " + e);
                }
            });
            throw new IllegalStateException("FX action timed out");
        }
        if (error[0] != null) {
            throw error[0];
        }
    }
}
