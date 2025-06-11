package com.WasteWise.WasteCollectionLogs.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.WasteWise.WasteCollectionLogs.Model.WasteLog;

@Repository
public interface WasteLogRepository extends JpaRepository<WasteLog,Long>{
    
    List<WasteLog>findByZoneIdAndCollectionStartTimeBetween(String zoneId,LocalDateTime startDate,LocalDateTime endTime);
    
//    List<WasteLog>findByCollectionStartTimeBetween(ZonedDateTime startDate, ZonedDateTime endTime);
    
    List<WasteLog> findByVehicleIdAndCollectionStartTimeBetween(String vehicleId, LocalDateTime startDateTime, LocalDateTime endDateTime);
    
//    @Query("SELECT MAX(w.logId) FROM WasteLog w")
//    String findMaxLogId(); // This will return "999" if "999" is the max, or "100" if "100" is max.
}
