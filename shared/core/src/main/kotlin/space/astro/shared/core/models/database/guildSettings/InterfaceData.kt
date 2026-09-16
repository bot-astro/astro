package space.astro.shared.core.models.database.guildSettings

import net.dv8tion.jda.api.entities.MessageEmbed
import space.astro.shared.core.utils.extensions.isValidSnowflake
import space.astro.shared.core.utils.validation.ValidationResult
import space.astro.shared.core.utils.validation.asValidationResult

data class InterfaceData(
    var channelID: String,
    var messageID: String,
    var buttons: MutableList<InterfaceButton> = mutableListOf(),
    val embedStyle: EmbedStyle,
    var generateEmbedFields: Boolean,
) {
    fun validate() : ValidationResult {
        val channelIdValidation = channelID.isValidSnowflake().asValidationResult("invalid channel id")
        val messageIdValidation = messageID.isValidSnowflake().asValidationResult("invalid message id")
        val buttonsValidation = buttons.map { it.validate() }.firstOrNull { !it.isValid } ?: ValidationResult.valid()
        val embedStyleValidation = embedStyle.validate()

        return ValidationResult.combine(
            channelIdValidation,
            messageIdValidation,
            buttonsValidation,
            embedStyleValidation
        )
    }


    data class EmbedStyle(
        var url: String?,
        var title: String?,
        var description: String?,
        var timestamp: Long?,
        var color: Int,
        var thumbnail: String?,
        var image: String?,
        var authorName: String?,
        var authorUrl: String?,
        var authorIconUrl: String?,
        var footer: String?,
        var footerIconUrl: String?
    ) {
        fun validate() : ValidationResult {
            val urlValidation = (url?.length?.let { it < MessageEmbed.URL_MAX_LENGTH } ?: true).asValidationResult("the url for the embed must be less than ${MessageEmbed.URL_MAX_LENGTH} characters")
            val titleValidation = (title?.length?.let { it < MessageEmbed.TITLE_MAX_LENGTH } ?: true).asValidationResult("the title for the embed must be less than ${MessageEmbed.TITLE_MAX_LENGTH} characters")
            val descriptionValidation = (description?.length?.let { it < MessageEmbed.DESCRIPTION_MAX_LENGTH } ?: true).asValidationResult("the description for the embed must be less than ${MessageEmbed.DESCRIPTION_MAX_LENGTH} characters")
            val thumbnailValidation = (thumbnail?.length?.let { it < MessageEmbed.URL_MAX_LENGTH } ?: true).asValidationResult("the thumbnail for the embed must be less than ${MessageEmbed.URL_MAX_LENGTH} characters")
            val imageValidation = (image?.length?.let { it < MessageEmbed.URL_MAX_LENGTH } ?: true).asValidationResult("the image for the embed must be less than ${MessageEmbed.URL_MAX_LENGTH} characters")
            val authorNameValidation = (authorName?.length?.let { it < MessageEmbed.AUTHOR_MAX_LENGTH } ?: true).asValidationResult("the author name for the embed must be less than ${MessageEmbed.AUTHOR_MAX_LENGTH} characters")
            val authorUrlValidation = (authorUrl?.length?.let { it < MessageEmbed.URL_MAX_LENGTH } ?: true).asValidationResult("the author url for the embed must be less than ${MessageEmbed.URL_MAX_LENGTH} characters")
            val authorIconUrlValidation = (authorIconUrl?.length?.let { it < MessageEmbed.URL_MAX_LENGTH } ?: true).asValidationResult("the author icon url for the embed must be less than ${MessageEmbed.URL_MAX_LENGTH} characters")
            val footerValidation = (footer?.length?.let { it < MessageEmbed.TEXT_MAX_LENGTH } ?: true).asValidationResult("the footer for the embed must be less than ${MessageEmbed.TEXT_MAX_LENGTH} characters")
            val footerIconUrlValidation = (footerIconUrl?.length?.let { it < MessageEmbed.URL_MAX_LENGTH } ?: true).asValidationResult("the footer icon url for the embed must be less than ${MessageEmbed.URL_MAX_LENGTH} characters")

            return ValidationResult.combine(
                urlValidation,
                titleValidation,
                descriptionValidation,
                thumbnailValidation,
                imageValidation,
                authorNameValidation,
                authorUrlValidation,
                authorIconUrlValidation,
                footerValidation,
                footerIconUrlValidation
            )
        }
    }


    data class InterfaceButton(
        var id: String,
        var name: String?,
        var emoji: String?,
        var buttonStyleKey: Int,
        var buttonDisabled: Boolean,
        var position: Pair<Int, Int>,
        var fieldValue: String = id
    ) {
        fun validate() : ValidationResult {
            val firstPositionValidation = (position.first in 0..4).asValidationResult("invalid button starting position, must be between 0 and 4")
            val secondPositionValidation = (position.second in 0..4).asValidationResult("invalid button ending position, must be between 0 and 4")

            return ValidationResult.combine(
                firstPositionValidation,
                secondPositionValidation
            )
        }
    }
}