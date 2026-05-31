package za.gov.municipal.ictasset.domain.usecase

import za.gov.municipal.ictasset.domain.model.RegisterAssetRequest
import za.gov.municipal.ictasset.domain.model.SaveResult
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.repository.AssetRepository

class RegisterAssetUseCase(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(request: RegisterAssetRequest, actor: User): SaveResult {
        if (!actor.role.canWriteAssets) {
            return SaveResult.Error("Your role cannot register assets.")
        }
        return assetRepository.registerAsset(request, actor)
    }
}
