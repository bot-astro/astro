rootProject.name = "astro"

// Include gradle modules
include(":services:central-api")
include(":shared:core")
include("services:bot")
findProject(":services:bot")?.name = "bot"
