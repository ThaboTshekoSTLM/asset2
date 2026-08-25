package za.gov.municipal.ictasset.presentation.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.gov.municipal.ictasset.domain.model.MovementType
import za.gov.municipal.ictasset.domain.model.ReferenceData
import za.gov.municipal.ictasset.domain.model.RegisterAssetRequest
import za.gov.municipal.ictasset.domain.model.SaveResult
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.repository.ReferenceRepository
import za.gov.municipal.ictasset.domain.usecase.RegisterAssetUseCase

data class RegistrationFormState(
    val deviceDescription: String = "",
    val assetBarcode: String = "",
    val serialNumber: String = "",
    val departmentId: Long? = null,
    val section: String = "",
    val buildingId: Long? = null,
    val officeNumber: String = "",
    val roomId: Long? = null,
    val roomBarcode: String = "",
    val currentOwner: String = "",
    val previousOwner: String = "Stores",
    val dateRegistered: Long = System.currentTimeMillis(),
    val dateMoved: Long = System.currentTimeMillis(),
    val movementType: MovementType = MovementType.NEW_ALLOCATION,
    val notes: String = "",
    val assetPhotoPath: String = "",
    val message: String? = null,
    val saving: Boolean = false
)

class RegistrationViewModel(
    private val registerAssetUseCase: RegisterAssetUseCase,
    private val referenceRepository: ReferenceRepository
) : ViewModel() {
    private val _formState = MutableStateFlow(RegistrationFormState())
    val formState: StateFlow<RegistrationFormState> = _formState.asStateFlow()

    val referenceData: StateFlow<ReferenceData> =
        referenceRepository.observeReferenceData()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReferenceData(emptyList(), emptyList(), emptyList()))

    fun update(block: (RegistrationFormState) -> RegistrationFormState) {
        _formState.update { block(it).copy(message = null) }
    }

    fun setAssetBarcode(value: String) {
        update { it.copy(assetBarcode = value.trim().uppercase()) }
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
                        buildingId = room.buildingId,
                        officeNumber = room.officeNumber,
                        message = "Room details filled from barcode."
                    )
                }
            }
        }
    }

    fun save(actor: User) {
        val state = _formState.value
        if (state.saving) return

        // Lock the form before launching the coroutine. Setting this inside the
        // coroutine leaves a small window where two quick taps can submit the
        // same barcode twice.
        _formState.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            val result = registerAssetUseCase(
                RegisterAssetRequest(
                    deviceDescription = state.deviceDescription,
                    assetBarcode = state.assetBarcode,
                    serialNumber = state.serialNumber,
                    departmentId = state.departmentId,
                    section = state.section,
                    buildingId = state.buildingId,
                    officeNumber = state.officeNumber,
                    roomId = state.roomId,
                    roomBarcode = state.roomBarcode,
                    currentOwner = state.currentOwner,
                    previousOwner = state.previousOwner,
                    technicianResponsibleId = actor.id,
                    dateRegistered = state.dateRegistered,
                    dateMoved = state.dateMoved,
                    movementType = state.movementType,
                    notes = state.notes,
                    assetPhotoPath = state.assetPhotoPath
                ),
                actor
            )
            _formState.update {
                when (result) {
                    is SaveResult.Success -> RegistrationFormState(
                        message = "Asset registered successfully."
                    )
                    is SaveResult.Error -> it.copy(saving = false, message = result.message)
                }
            }
        }
    }
}
