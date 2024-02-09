rootProject.name = "astro"

// Include gradle modules
include(":services:central-api")
include(":shared:core")
include("services:bot")
findProject(":services:bot")?.name = "bot"
include("services:support-bot")
findProject(":services:support-bot")?.name = "support-bot"
include("services:please-bot")
findProject(":services:please-bot")?.name = "please-bot"
include("services:entitlements-expiration-job")
findProject(":services:entitlements-expiration-job")?.name = "entitlements-expiration-job"
