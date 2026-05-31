package za.gov.municipal.ictasset.domain.repository

import kotlinx.coroutines.flow.Flow
import za.gov.municipal.ictasset.domain.model.ReferenceData
import za.gov.municipal.ictasset.domain.model.Room

interface ReferenceRepository {
    fun observeReferenceData(): Flow<ReferenceData>
    suspend fun findRoomByBarcode(roomBarcode: String): Room?
}
