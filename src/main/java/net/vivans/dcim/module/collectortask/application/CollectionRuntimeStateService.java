package net.vivans.dcim.module.collectortask.application;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import net.vivans.dcim.module.collectortask.infrastructure.collector.*;
@Service @RequiredArgsConstructor
public class CollectionRuntimeStateService {
    private final CollectorJobClient collector;
    private final SystemSettingService settings;
    public record Key(String jobId,Integer deviceId) {}
    public Map<Key,CollectorDeviceResultResponse> load(Collection<Integer> ids){
        if(ids==null || ids.isEmpty())return Map.of();
        List<Integer> unique=ids.stream().filter(Objects::nonNull).distinct().toList();
        Map<Key,CollectorDeviceResultResponse> result=new HashMap<>();
        for(int start=0;start<unique.size();start+=500)
            for(var row:collector.deviceResults(unique.subList(start,Math.min(start+500,unique.size()))))
                if(row.collectorJobId()!=null && row.deviceId()!=null)result.put(new Key(row.collectorJobId(),row.deviceId()),row);
        return result;
    }
    public CollectionRuntimeState state(Map<Key,CollectorDeviceResultResponse> rows,String jobId,Integer id){
        int threshold=settings.failureThreshold();
        CollectorDeviceResultResponse result=jobId==null?null:rows.get(new Key(jobId,id));
        if(result==null || !result.jobEnabled())return new CollectionRuntimeState("UNKNOWN",0,threshold,null,null,null,null,"수집기 상태 확인 불가",null,false);
        String state,message;
        if(result.lastAttemptAt()==null){state="PENDING";message="시작 대기";}
        else if(result.consecutiveFailures()>=threshold){state="COMM_ERROR";message="통신 오류(연속 "+result.consecutiveFailures()+"회)";}
        else if(result.consecutiveFailures()>0){state="FAILING";message="최근 수집 실패("+result.consecutiveFailures()+"회)";}
        else if("PARTIAL".equals(result.lastOutcome())){state="PARTIAL";message="일부 응답";}
        else if("SUCCESS".equals(result.lastOutcome())){state="OK";message="정상 응답";}
        else{state="PENDING";message="시작 대기";}
        String code=Set.of("FAILING","COMM_ERROR","PARTIAL").contains(state)?result.lastFailureCode():null;
        if(code!=null)message+=" · "+failureMessage(code);
        return new CollectionRuntimeState(state,result.consecutiveFailures(),threshold,result.lastAttemptAt(),result.lastSuccessAt(),result.lastFailureAt(),code,message,result.backoffUntil(),result.inFlight());
    }
    private static String failureMessage(String code){return switch(code){
        case "TIMEOUT"->"응답 시간 초과";case "UNREACHABLE"->"연결 불가";case "DEVICE_ERROR"->"장비 오류 응답";
        case "NO_POINTS"->"측정 항목 없음";case "OVERRUN"->"이전 수집 미완료";default->"수집 오류";
    };}
}
