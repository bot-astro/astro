package space.astro.bot.interactions.handlers.command.impl.predashboard

import com.chargebee.models.Subscription
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import space.astro.bot.core.ui.Buttons
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.SettingsInteractionContext
import space.astro.bot.interactions.handlers.command.AbstractCommand
import space.astro.bot.interactions.handlers.command.Command
import space.astro.bot.interactions.handlers.command.SubCommand
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.UserDao
import space.astro.shared.core.models.database.GuildUpgradeData
import space.astro.shared.core.services.chargebee.ChargebeeClientService

@Command(
    name = "premium",
    description = "Get info about premium",
)
class PremiumCommand(
    private val chargebeeClientService: ChargebeeClientService,
    private val userDao: UserDao,
    private val guildDao: GuildDao
) : AbstractCommand() {
    ////////////
    /// INFO ///
    ////////////
    @SubCommand(
        name = "info",
        description = "Everything you need to know about premium"
    )
    suspend fun info(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {
        ctx.replyHandler.replyEmbedAndComponent(
            embed = Embeds.helpPremium,
            component = Buttons.premium
        )
    }

    //////////////////
    /// DEPRECATED ///
    //////////////////
    @SubCommand(
        name = "upgrade",
        description = "Upgrade a server to premium using the old subscription system",
        group = "deprecated",
        groupDescription = "0"
    )
    suspend fun deprecatedUpgrade(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext,
    ) {
        val guild = ctx.guild

        val guildDto = ctx.guildData

        if (guildDto.upgradedByUserID != null) {
            ctx.replyHandler.replyEmbed(
                Embeds.error(
                    "This server is already upgraded to premium by someone."
                )
            )
            return
        }


        val userSubs = chargebeeClientService.getActiveServerSubscriptionsOfUser(ctx.memberId)
        val userUpgrades = ctx.userData.guildActiveUpgrades

        if (userSubs.isEmpty()) {
            ctx.replyHandler.replyEmbedAndComponent(
                Embeds.error(
                    "You don't have any active ${Emojis.premium.formatted} premium subscription so you can't upgrade any server." +
                    "\nUse the button below to get a premium subscription for your server."
                ),
                Buttons.premium
            )
            return
        }

        // Count the number of used subscriptions from the user
        val upgradesMap: MutableMap<String, Int> = mutableMapOf()
        for (upgrade: GuildUpgradeData in userUpgrades)
            upgradesMap[upgrade.subscriptionID] = upgradesMap[upgrade.subscriptionID]?.plus(1) ?: 1

        val subscriptionQuantities: MutableMap<String, Int> = mutableMapOf()
        for (sub in userSubs)
            subscriptionQuantities[sub.subscription().id()] = sub.subscription().subscriptionItems().first().quantity()

        val availableSubToUse: LinkedHashMap<String, Int> = linkedMapOf()
        subscriptionQuantities.forEach {
            val availableNumber = it.value - (upgradesMap[it.key] ?: 0)
            if (availableNumber > 0)
                availableSubToUse[it.key] = availableNumber
        }

        if (availableSubToUse.isEmpty()) {
            ctx.replyHandler.replyEmbedAndComponent(
                Embeds.error(
                    "You already used all your available upgrades." +
                    "\nYou can use `/premium deprecated downgrade` to move your premium subscriptions from another server to this one or you can get another premium subscription for this server, see `/premium info`."
                ),
                Buttons.premium
            )
            return
        }

        val descriptionBuilder = StringBuilder()
        descriptionBuilder.append("Choose which one of the following subscription to use via the menu below this message.")


        val options = mutableListOf<SelectOption>()
        var counter = 0
        for ((subID, quantity) in availableSubToUse) {
            counter++
            val sub = userSubs.first { it.subscription().id() == subID }
            val text = "`${counter}.` **${
                if (sub.subscription()
                        .billingPeriodUnit() == Subscription.BillingPeriodUnit.YEAR
                ) "Yearly" else "Monthly"
            }** subscription with **${
                sub.subscription().subscriptionItems().first().quantity()
            } upgrades** (*${
                sub.subscription().subscriptionItems().first().quantity() - quantity
            } used*, **$quantity available**)"
            descriptionBuilder.append("\n$text")
        }

        counter = 0
        for ((subID, quantity) in availableSubToUse) {
            counter++
            val sub = userSubs.first { it.subscription().id() == subID }
            val text = "${counter}. ${
                if (sub.subscription()
                        .billingPeriodUnit() == Subscription.BillingPeriodUnit.YEAR
                ) "Yearly" else "Monthly"
            } subscription with ${
                sub.subscription().subscriptionItems().first().quantity()
            } upgrades (${
                sub.subscription().subscriptionItems().first().quantity() - quantity
            } used, $quantity available)"
            options.add(SelectOption.of(text, (counter - 1).toString()))
        }

        val selectMenu = StringSelectMenu.create(InteractionIds.getRandom())
            .setPlaceholder("Select the subscription to use")
            .addOptions(options)
            .build()

        ctx.replyHandler.replyWithSelectMenu(
            Embeds.selector(descriptionBuilder.toString()),
            selectMenu,
            true
        ) { selections ->
            val subIndex = selections.first().toInt()
            val subSelected = userSubs.first { it.subscription().id() == availableSubToUse.keys.toList()[subIndex] }

            ctx.userData.guildActiveUpgrades.add(
                GuildUpgradeData(
                    guild.id,
                    subSelected.subscription().id(),
                    subSelected.subscription().billingPeriodUnit() == Subscription.BillingPeriodUnit.YEAR
                )
            )

            userDao.save(ctx.userData)
            ctx.guildData.upgradedByUserID = ctx.memberId
            guildDao.save(ctx.guildData)

            ctx.replyHandler.replyEmbed(Embeds.success(
                "Server successfully upgraded with a ${
                    if (subSelected.subscription().billingPeriodUnit() == Subscription.BillingPeriodUnit.YEAR)
                        "yearly"
                    else
                        "monthly"
                } subscription, enjoy :)" +
                        "\n\n*You can downgrade this server at any time via the `/premium deprecated downgrade` command.*"
            )
            )
        }
    }

    @SubCommand(
        name = "downgrade",
        description = "Downgrade a server from premium using the old subscription system",
        group = "deprecated",
        groupDescription = "0"
    )
    suspend fun deprecatedDowngrade(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {
        if (ctx.guildData.upgradedByUserID != ctx.memberId) {
            ctx.replyHandler.replyEmbed(
                Embeds.error(
                    "You did not upgrade the server `${ctx.guild.name}` [*${ctx.guildId}*] so you cannot downgrade it."
                )
            )
        }

        ctx.guildData.upgradedByUserID = null
        guildDao.save(ctx.guildData)
        ctx.userData.guildActiveUpgrades.removeIf { it.guildID == ctx.guildId }
        userDao.save(ctx.userData)

        ctx.replyHandler.replyEmbed(
            Embeds.success(
                "Server successfully downgraded from premium." +
                "Upgrade another server with `/premium deprecated upgrade`"
            ),
        )
    }
}