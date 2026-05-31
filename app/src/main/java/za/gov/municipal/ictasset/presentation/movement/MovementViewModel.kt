package za.gov.municipal.ictasset.presentation.movement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.gov.municipal.ictasset.domain.model.Asset
import za.gov.municipal.ictasset.domain.model.AssetMovement
import za.gov.municipal.ictasset.domain.model.MoveAssetRequest
import za.gov.municipal.ictasset.domain.model.MovementType
import za.gov.municipal.ictasset.domain.model.ReferenceData
import za.gov.municipal.ictasset.domain.model.SaveResult
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.repository.AssetRepository
import za.gov.municipal.ictasset.domain.repository.ReferenceRepository
import za.gov.municipal.ictasset.domain.usecase.MoveAssetUseCase

data class MovementFormState(
    val assetBarcode: String = "",
    val selectedAsset: Asset? = null,
    val newOwner: String = "",
    val newBuildingId: Long? = null,
    val newOfficeNumber: String = "",
    val departmentId: Long? = null,
    val section: String = "",
    val roomId: Long? = null,
    val roomBarcode: String = "",
    val movementDate: Long = System.currentTimeMillis(),
    val movementType: MovementType = MovementType.TRANSFER,
    val reason: String = "",
    val signatureConfirmation: String = "",
    val assetPhotoPath: String = "",
    val message: String? = null,
    val saving: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MovementViewModel(
    private val assetRepository: AssetRepository,
    private val referenceRepository: ReferenceRepository,
    private val moveAssetUseCase: MoveAssetUseCase
) : ViewModel() {
    private val _formState = MutableStateFlow(MovementFormState())
    val formState: StateFlow<MovementFormState> = _formState.asStateFlow()

    val referenceData: StateFlow<ReferenceData> =
        referenceRepository.observeReferenceData()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReferenceData(emptyList(), emptyList(), emptyList()))

    val history: StateFlow<List<AssetMovement>> =
        _formState
            .map { it.assetBarcode }
            .distinctUntilChanged()
            .flatMapLatest { barcode ->
                assetRepository.observeMovementHistoryByBarcode(barcode)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun update(block: (MovementFormState) -> MovementFormState) {
        _formState.update { block(it).copy(message = null) }
    }

    fun setAssetBarcode(value: String) {
        val barcode = value.trim().uppercase()
        _formState.update { it.copy(assetBarcode = barcode, message = null) }
        loadAsset(barcode)
    }

    fun loadAsset(barcode: String = _formState.value.assetBarcode) {
        if (barcode.isBlank()) return
        viewModelScope.launch {
            val asset = assetRepository.findAssetByBarcode(barcode)
            if (asset == null) {
                _formState.update { it.copy(selectedAsset = null, message = "Asset barcode not found.") }
            } else {
                _formState.update {
                    it.copy(
                        selectedAsset = asset,
                        newOwner = asset.currentOwner,
                        newBuildingId = asset.buildingId,
                        newOfficeNumber = asset.officeNumber,
                        departmentId = asset.departmentId,
                        section = asset.section,
                        roomId = asset.roomId,
                        roomBarcode = asset.roomBarcode.orEmpty(),
                        assetPhotoPath = asset.assetPhotoPath.orEmpty(),
                        message = "Asset details loaded."
                    )
                }
            }
        }
    }

    fun applyRoomBarcode(value: String) {
        viewModelScope.launch {
            val room = referenceRepository.findRoomByBarcode(value)
            if (room == null) {
                _formState.update { it.copy(roomBarcode = value, message = "Room barcode not found.") }
            } else {
                _formState.update {
                    it.copy(
                        roomId = room.id,
                        roomBarcode = room.roomBarcode,
                        newBuildingId = room.buildingId,
                        newOfficeNumber = room.officeNumber,
                        message = "New location filled from room barcode."
                    )
                }
            }
        }
    }

    fun save(actor: User) {
        val state = _formState.value
        viewModelScope.launch {
            _formState.update { it.copy(saving = true, message = null) }
            val result = moveAssetUseCase(
                MoveAssetRequest(
                    assetBarcode = state.assetBarcode,
                    technicianUserId = actor.id,
                    newOwner = state.newOwner,
                    newBuildingId = state.newBuildingId,
                    newOfficeNumber = state.newOfficeNumber,
                    departmentId = state.departmentId,
                    section = state.section,
                    roomId = state.roomId,
                    roomBarcode = state.roomBarcode,
                    movementDate = state.movementDate,
                    movementType = state.movementType,
                    reason = state.reason,
                    signatureConfirmation = state.signatureConfirmation,
                    assetPhotoPath = state.assetPhotoPath
                ),
                actor
            )
            _formState.update {
                when (result) {
                    is SaveResult.Success -> MovementFormState(
                        message = "Movement recorded successfully."
                    )
                    is SaveResult.Error -> it.copy(saving = false, message = result.message)
                }
            }
        }
    }
}
