(function(global){
 'use strict';
 const groups=[
  {key:'home',label:'한눈에 보기',items:[['home','운영 요약']]},
  {key:'devices',label:'장비 관리',items:[['devices','장비 목록'],['device-groups','장비 그룹'],['models','장비 모델'],['gpu-agent-pending','GPU 에이전트 등록 대기']]},
  {key:'assets',label:'자산 · 랙',items:[['assets','장비 자산'],['rack-overview','랙 현황'],['scene-models','3D 모델 · 건축 자산']]},
  {key:'collection-status',label:'데이터 수집',items:[['collection-status','수집 상태'],['tasks','수집 작업'],['lora-collection-status','MQTT 저장 상태'],['live-telemetry','실시간 값']],advanced:[['lora-mqtt-sources','MQTT 연결'],['lora-endpoints','장비 식별자'],['lora-model-mapping','수집 항목 매핑'],['mqtt-monitor','메시지 진단']]},
  {key:'alarms',label:'알람',items:[['alarms','알람 이력'],['alarm-rules','임계값 규칙']]},
  {key:'locations',label:'설정',items:[['locations','위치 · 공간'],['device-pages','화면 구성'],['codes','공통코드'],['users','사용자']]},
  {key:'notes',label:'사용 가이드',items:[['notes','사용 가이드']]}
 ];
 function pageTab(code){return `page-${String(code||'').toLowerCase().replace(/[^a-z0-9_-]/g,'-')}`;}
 function setDynamicPages(pages){
  for(let i=groups.length-1;i>=0;i--) if(groups[i].dynamicPage) groups.splice(i,1);
  for(const page of pages||[]){
   const tab=page.tab||pageTab(page.code), section=page.section||'monitoring';
   const sectionLabel=section==='operations'?'운영 관리':section==='settings'?'관리':'모니터링';
   groups.push({key:tab,label:sectionLabel,items:[[tab,page.name]],dynamicPage:true,pageCode:page.code});
  }
 }
 function groupFor(tab){return groups.find(group=>[...group.items,...(group.advanced||[])].some(item=>item[0]===tab));}
 function labelFor(tab){return groups.flatMap(group=>[...group.items,...(group.advanced||[])]).find(item=>item[0]===tab)?.[1]||'';}
 global.OpsNavigation={groups,groupFor,labelFor,pageTab,setDynamicPages};
})(globalThis);
