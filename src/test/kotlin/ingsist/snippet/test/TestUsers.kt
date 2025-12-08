package ingsist.snippet.test

data class TestUser(
    val id: String,
    val token: String = "token-$id",
)

object TestUsers {
    val OWNER = TestUser("owner-user-id", "owner-token")
    val COLLABORATOR = TestUser("collaborator-user-id", "collab-token")
    val STRANGER = TestUser("stranger-user-id", "stranger-token")
}
