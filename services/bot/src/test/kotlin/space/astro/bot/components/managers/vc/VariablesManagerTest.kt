package space.astro.bot.components.managers.vc

import net.dv8tion.jda.api.Permission
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import space.astro.shared.core.components.managers.VariablesManager

class VariablesManagerTest {

    @Test
    fun testPremiumVariablesRegexMatching() {
        val shouldNotMatch = "{nickname} sample name"
        val shouldMatch = "cool {n}"
        val shouldMatch2 = "nice {activity_name}"

        assertFalse(VariablesManager.Checkers.containsPremiumVariable(shouldNotMatch))
        assert(VariablesManager.Checkers.containsPremiumVariable(shouldMatch))
        assert(VariablesManager.Checkers.containsPremiumVariable(shouldMatch2))
    }
}