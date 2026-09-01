package space.astro.shared.core.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import space.astro.shared.core.models.database.UserEntity

interface UserRepository: MongoRepository<UserEntity, String> {
    fun findByUserID(userID: String): UserEntity?
}