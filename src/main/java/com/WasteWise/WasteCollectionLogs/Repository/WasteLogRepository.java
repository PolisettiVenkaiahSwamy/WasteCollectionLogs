package com.WasteWise.WasteCollectionLogs.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.WasteWise.WasteCollectionLogs.Model.WasteLog;

@Repository
public interface WasteLogRepository extends JpaRepository<WasteLog,Long>{
    
    List<WasteLog>findByZoneIdAndCollectionStartTimeBetween(String zoneId,LocalDateTime startDate,LocalDateTime endTime);
    
    
    List<WasteLog> findByVehicleIdAndCollectionStartTimeBetween(String vehicleId, LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    Optional<WasteLog> findByWorkerIdAndZoneIdAndVehicleIdAndCollectionEndTimeIsNull(String workerId, String zoneId, String vehicleId);
}
