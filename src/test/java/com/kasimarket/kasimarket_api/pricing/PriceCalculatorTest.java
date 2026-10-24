package com.kasimarket.kasimarket_api.pricing;

import com.kasimarket.kasimarket_api.catalog.TaskType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// A unit test checks that one small piece of code gives the right answer.
// Each method marked @Test is one check. If any check fails, we know the maths is broken.
class PriceCalculatorTest {

    // The calculator under test, with a 20 percent commission.
    // We create it by hand, so no database or web server is needed.
    private final PriceCalculator calculator = new PriceCalculator(0.20);

    // A helper that builds a task, so we do not repeat the same lines in every test.
    private TaskType task(String name, int ratePerHour, int minHours) {
        TaskType task = new TaskType();
        task.setName(name);
        task.setBaseRatePerHour(ratePerHour);
        task.setMinHours(minHours);
        return task;
    }

    @Test
    void dogWalkingForOneHourCostsR80() {
        PriceQuote quote = calculator.calculate(task("Dog walking", 80, 1), 1, false);

        assertThat(quote.totalPrice()).isEqualTo(80);
        assertThat(quote.platformFee()).isEqualTo(16);    // 20 percent of 80
        assertThat(quote.workerEarning()).isEqualTo(64);  // 80 minus 16
    }

    @Test
    void shortBookingIsChargedAtTheTaskMinimum() {
        // Babysitting needs at least 3 hours, so asking for 2 is charged as 3.
        PriceQuote quote = calculator.calculate(task("Babysitting", 60, 3), 2, false);

        assertThat(quote.hours()).isEqualTo(3);
        assertThat(quote.totalPrice()).isEqualTo(180);    // 60 x 3
    }

    @Test
    void urgentJobsCostThirtyPercentMore() {
        PriceQuote quote = calculator.calculate(task("Dog walking", 80, 1), 1, true);

        assertThat(quote.totalPrice()).isEqualTo(104);    // 80 x 1.3
        assertThat(quote.platformFee()).isEqualTo(21);    // 20 percent of 104 = 20.8, rounded up
        assertThat(quote.workerEarning()).isEqualTo(83);
    }

    @Test
    void feeAndWorkerEarningAlwaysAddUpToTheTotal() {
        PriceQuote quote = calculator.calculate(task("Deep clean", 87, 6), 6, false);

        assertThat(quote.totalPrice()).isEqualTo(522);    // 87 x 6
        // No money may appear or disappear when we split it.
        assertThat(quote.platformFee() + quote.workerEarning()).isEqualTo(quote.totalPrice());
    }

    @Test
    void zeroHoursIsRejected() {
        // We expect an error here. The test passes only if the error happens.
        assertThatThrownBy(() -> calculator.calculate(task("Errands", 90, 1), 0, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
