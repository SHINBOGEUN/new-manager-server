(function(global){
 'use strict';
 const groups=[
  {key:'home',label:'한눈에 보기',items:[['home','운영 요약']]},
  {key:'dashboard-page',label:'대시보드',items:[['dashboard-page','대시보드']]},
  {key:'environment-page',label:'공간 · 환경',items:[['environment-page','공간 · 환경']]},
  {key:'cooling-page',label:'냉각 모니터링',items:[['cooling-page','냉각 모니터링']]},
  {key:'devices',label:'장비 관리',items:[['devices','장비 목록'],['wizard','장비 등록'],['device-groups','장비 그룹']]},
  {key:'assets',label:'자산 · 랙',items:[['assets','장비 자산'],['rack-overview','랙 현황'],['scene-models','3D 모델 · 건축 자산']]},
  {key:'collection-status',label:'데이터 수집',items:[['collection-status','수집 상태'],['tasks','수집 작업'],['lora-collection-status','MQTT 저장 상태'],['live-telemetry','실시간 값']],advanced:[['lora-mqtt-sources','MQTT 연결'],['lora-endpoints','장비 식별자'],['lora-model-mapping','수집 항목 매핑'],['mqtt-monitor','메시지 진단']]},
  {key:'alarms',label:'알람',items:[['alarms','알람 · 경보 기준']]},
  {key:'analysis',label:'데이터 분석',items:[['analysis','데이터 분석']]},
  {key:'locations',label:'설정',items:[['locations','위치 · 공간'],['models','장비 모델'],['device-pages','화면 구성'],['codes','공통코드'],['users','사용자']]},
  {key:'notes',label:'사용 가이드',items:[['notes','사용 가이드']]}
 ];
 function groupFor(tab){return groups.find(group=>[...group.items,...(group.advanced||[])].some(item=>item[0]===tab));}
 function labelFor(tab){return groups.flatMap(group=>[...group.items,...(group.advanced||[])]).find(item=>item[0]===tab)?.[1]||'';}
 global.OpsNavigation={groups,groupFor,labelFor};
})(globalThis);
