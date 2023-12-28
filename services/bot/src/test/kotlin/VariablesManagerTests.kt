import space.astro.bot.components.managers.vc.VariablesManager
import kotlin.test.Test
import kotlin.test.assertFalse

class VariablesManagerTests {
    @Test
    fun testPremiumVariablesRegexMatching() {
        val shouldNotMatch = "sample name"
        val shouldMatch = "cool {n}"
        val shouldMatch2 = "nice {activity_name}"

        assertFalse(VariablesManager.Checkers.containsPremiumVariable(shouldNotMatch))
        assert(VariablesManager.Checkers.containsPremiumVariable(shouldMatch))
        assert(VariablesManager.Checkers.containsPremiumVariable(shouldMatch2))
    }
}