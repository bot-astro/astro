package space.astro.shared.core.repositories.mongo

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository
import space.astro.shared.core.models.database.UserEntity

interface UserRepository : MongoRepository<UserEntity, ObjectId> {
    fun findByUserID(userID: String): UserEntity?
}
