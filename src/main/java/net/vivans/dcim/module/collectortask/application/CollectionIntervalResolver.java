package net.vivans.dcim.module.collectortask.application;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.transaction.annotation.Transactional;
import net.vivans.dcim.module.common.domain.model.CommonCode;
import net.vivans.dcim.module.common.domain.repository.CommonCodeRepository;
@Service @RequiredArgsConstructor @Slf4j @Transactional(readOnly=true)
public class CollectionIntervalResolver {
    public static final String GROUP="COLLECTION_INTERVAL";
    private final CommonCodeRepository codes;
    private final SystemSettingService settings;
    public record Interval(String code,String name,long seconds,String cron) {}
    public Interval resolve(CommonCode code) {
        if(!GROUP.equals(code.getCodeGroup().getGroupKey())) throw new IllegalArgumentException("수집 주기 코드가 필요합니다.");
        Duration d;
        try {d=Duration.parse(code.getCode());} catch(RuntimeException e){throw invalid();}
        if(!d.toString().equals(code.getCode()) || d.getNano()!=0 || d.getSeconds()<=0 || d.getSeconds()%60!=0) throw invalid();
        long m=d.getSeconds()/60;String cron;
        if(m<60 && 60%m==0) cron="0 */"+m+" * * * *";
        else if(m%60==0 && m<=1440 && 24%(m/60)==0) {
            long h=m/60;cron=h==24?"0 0 0 * * *":h==1?"0 0 * * * *":"0 0 */"+h+" * * *";
        } else throw invalid();
        return new Interval(code.getCode(),code.getName(),d.getSeconds(),cron);
    }
    public List<Interval> intervals() {
        return codes.findAll().stream().filter(c->GROUP.equals(c.getCodeGroup().getGroupKey()))
            .sorted(Comparator.comparing(CommonCode::getSortOrder,Comparator.nullsLast(Integer::compareTo)).thenComparing(CommonCode::getId))
            .flatMap(c->{try{return java.util.stream.Stream.of(resolve(c));}catch(IllegalArgumentException e){log.warn("invalid collection interval id={}",c.getId());return java.util.stream.Stream.empty();}}).toList();
    }
    public CommonCode code(String value) {
        CommonCode c=codes.findByCodeGroupGroupKeyAndCode(GROUP,value).orElseThrow(CollectionIntervalResolver::invalid);resolve(c);return c;
    }
    public CommonCode defaultCode() {
        String value=settings.getString(SystemSettingService.DEFAULT_INTERVAL);
        if(value!=null) try{return code(value);}catch(IllegalArgumentException e){log.warn("invalid default collection interval; using first valid preset");}
        return code(intervals().stream().findFirst().orElseThrow(CollectionIntervalResolver::invalid).code());
    }
    public Interval defaultInterval(){return resolve(defaultCode());}
    public static boolean equivalent(String a,String b) {
        try {
            CronExpression x=CronExpression.parse(a),y=CronExpression.parse(b);
            ZonedDateTime tx=ZonedDateTime.of(2030,1,1,0,0,0,0,ZoneOffset.UTC),ty=tx;
            for(int i=0;i<10;i++){tx=x.next(tx);ty=y.next(ty);if(tx==null || !tx.equals(ty))return false;}return true;
        } catch(RuntimeException e){return false;}
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("유효한 COLLECTION_INTERVAL 주기를 선택하세요.");}
}
