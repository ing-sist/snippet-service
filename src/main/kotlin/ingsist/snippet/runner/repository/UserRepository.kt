import ingsist.snippet.runner.domain.OwnerConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<OwnerConfig, String>
