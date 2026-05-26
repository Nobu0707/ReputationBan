package dev.modplugin.reputationban.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReportCategoryTest {
    @Test
    void trimsAndStoresValues() {
        ReportCategory category = new ReportCategory(" griefing ", " 荒らし ", 15, false);

        assertEquals("griefing", category.key());
        assertEquals("荒らし", category.displayName());
        assertEquals(15, category.deduction());
    }

    @Test
    void supportsStaffReviewRequiredCategories() {
        ReportCategory category = new ReportCategory("cheating", "チート疑い", 0, true);

        assertTrue(category.staffReviewRequired());
        assertEquals(0, category.deduction());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new ReportCategory("", "その他", 0, true));
        assertThrows(IllegalArgumentException.class, () -> new ReportCategory("other", "", 0, true));
        assertThrows(IllegalArgumentException.class, () -> new ReportCategory("spam", "スパム", -1, false));
    }
}
