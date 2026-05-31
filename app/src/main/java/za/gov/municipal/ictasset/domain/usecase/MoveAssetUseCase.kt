package za.gov.municipal.ictasset.domain.usecase

import za.gov.municipal.ictasset.domain.model.MoveAssetRequest
import za.gov.municipal.ictasset.domain.model.SaveResult
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.repository.AssetRepository

class MoveAssetUseCase(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(request: MoveAssetRequest, actor: User): SaveResult {
        if (!actor.role.canWriteAssets) {
            return SaveResult.Error("Your role cannot move or allocate assets.")
        }
        return assetRepository.moveAsset(request, actor)
    }
}
