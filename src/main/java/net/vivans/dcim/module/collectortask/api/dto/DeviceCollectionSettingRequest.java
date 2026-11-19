package net.vivans.dcim.module.collectortask.api.dto;
public record DeviceCollectionSettingRequest(Boolean enabled,String intervalCode,Integer expectedSilenceSeconds) {}
