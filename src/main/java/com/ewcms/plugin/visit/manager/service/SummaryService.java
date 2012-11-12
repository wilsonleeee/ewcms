package com.ewcms.plugin.visit.manager.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ewcms.common.lang.EmptyUtil;
import com.ewcms.plugin.visit.manager.dao.VisitDAO;
import com.ewcms.plugin.visit.manager.vo.InAndExitVo;
import com.ewcms.plugin.visit.manager.vo.LastVisitVo;
import com.ewcms.plugin.visit.manager.vo.OnlineVo;
import com.ewcms.plugin.visit.manager.vo.SummaryVo;
import com.ewcms.plugin.visit.util.ChartVisitUtil;
import com.ewcms.plugin.visit.util.DateTimeUtil;
import com.ewcms.plugin.visit.util.NumberUtil;

@Service
public class SummaryService implements SummaryServiceable {

	@Autowired
	private VisitDAO visitDAO;
	
	@Override
	public String findFirstDate(Integer siteId) {
		String firstDate = visitDAO.findFirstDate(siteId);
		if (EmptyUtil.isStringEmpty(firstDate)) return DateTimeUtil.getDateToString(DateTimeUtil.getCurrent());
		return firstDate;
	}

	@Override
	public Integer findDays(Integer siteId) {
		String firstAddDate = visitDAO.findFirstDate(siteId);
		if (EmptyUtil.isStringEmpty(firstAddDate)) return 1;
		Date first = DateTimeUtil.getStringToDate(firstAddDate);
		Date current = DateTimeUtil.getCurrent();
		return (int) ((current.getTime() - first.getTime())/(24*60*60*1000));  
	}

	@Override
	public List<SummaryVo> findSummaryTable(Integer siteId) {
		List<SummaryVo> list = new ArrayList<SummaryVo>();
		
		Date current = new Date(Calendar.getInstance().getTime().getTime());
		
		SummaryVo currentVo = assign("今日", siteId, current);
		list.add(currentVo);
		
		Date tomorrow = DateTimeUtil.getPreviousDay(current);
		SummaryVo tomorrowVo = assign("昨天", siteId, tomorrow);
		list.add(tomorrowVo);
		
		List<String> weekList = DateTimeUtil.getThisWeek();
		SummaryVo weekVo = assign("本周", siteId, weekList);
		list.add(weekVo);
		
		List<String> monthMap = DateTimeUtil.getThisMonth();
		SummaryVo monthVo = assign("本月", siteId, monthMap);
		list.add(monthVo);
		
		SummaryVo avgVo = new SummaryVo();
		avgVo.setName("平均");
		String startDate = visitDAO.findFirstDate(siteId);
		if (startDate != ""){
			String endDate = DateTimeUtil.getDateToString(current);
			List<String> allMap = DateTimeUtil.getDateArea(startDate, endDate);
			SummaryVo allVo = assign("全部", siteId, allMap);
			list.add(allVo);
			
			Integer day = endDate.compareTo(startDate) + 1;
			
			avgVo.setIp(allVo.getIp()/day);
			avgVo.setPv(allVo.getPv()/day);
			avgVo.setUv(allVo.getUv()/day);
			avgVo.setAvgTime("");
			avgVo.setRvRate("");
		}
		list.add(avgVo);
	
		return list;
	}

	@Override
	public String findSummaryReport(String startDate, String endDate, Integer labelCount, Integer siteId) {
		List<String> categories = DateTimeUtil.getDateArea(startDate, endDate);
		Map<String, Map<String, Long>> dataSet = new LinkedHashMap<String, Map<String, Long>>();
		Map<String, Long> mapIP = findSummaryIp(categories, siteId);
		dataSet.put("IP", mapIP);
		Map<String, Long> mapUV = findSummaryUv(categories, siteId);
		dataSet.put("UV", mapUV);
		Map<String, Long> mapPV = findSummaryPv(categories, siteId);
		dataSet.put("PV", mapPV);
		
		return ChartVisitUtil.getLine2DChart(categories, dataSet, labelCount);
	}

	@Override
	public List<SummaryVo> findSiteTable(String startDate, String endDate, Integer siteId) {
		List<SummaryVo> list = new ArrayList<SummaryVo>();
		List<String> dateArea = DateTimeUtil.getDateArea(startDate, endDate);
		SummaryVo vo = null;
		for (String dateValue : dateArea){
			Date date = DateTimeUtil.getStringToDate(dateValue);
			vo = assign(dateValue, siteId, date);
			list.add(vo);
		}
		return list;
	}

	@Override
	public List<LastVisitVo> findLastTable(String startDate, String endDate, Integer rows, Integer siteId) {
		return visitDAO.findLastAccess(DateTimeUtil.getStringToDate(startDate), DateTimeUtil.getStringToDate(endDate), rows, siteId);
	}

	@Override
	public List<SummaryVo> findHourTable(String startDate, String endDate, Integer siteId) {
		List<SummaryVo> list = new ArrayList<SummaryVo>();
		List<String> dateArea = DateTimeUtil.getDateArea(startDate, endDate);
		
		list.add(null);
		
		SummaryVo vo = null;
		Long sumIp = 0L, sumUv = 0L, sumPv = 0L, sumRv = 0L;
		for (int i = 0; i <= 23; i++){
			String start = "" + i + ":00";
			String end = "" + i + ":59";
			vo = new SummaryVo();
			vo.setName(start + " —— " + end);
			Long countIp = 0L, countUv = 0L, countPv = 0L, countRv = 0L;
			for (String dateValue : dateArea){
				Date date = DateTimeUtil.getStringToDate(dateValue);
				Date startTime = DateTimeUtil.getStringToTime(start);
				Date endTime = DateTimeUtil.getStringToTime(end);
				
				countIp += visitDAO.findIpCountInHour(date, startTime, endTime, siteId);
				countUv += visitDAO.findUvCountInHour(date, startTime, endTime, siteId);
				countPv += visitDAO.findPvCountInHour(date, startTime, endTime, siteId);
				countRv += visitDAO.findRvCountInHour(date, startTime, endTime, siteId);
			}
			vo.setIp(countIp);
			vo.setUv(countUv);
			vo.setPv(countPv);
			vo.setRv(countRv);
			list.add(vo);
			
			sumIp += countIp;
			sumUv += countUv;
			sumPv += countPv;
			sumRv += countRv;
		}
		
		SummaryVo sumVo = new SummaryVo();
		sumVo.setName("总计");
		sumVo.setIp(sumIp);
		sumVo.setUv(sumUv);
		sumVo.setPv(sumPv);
		sumVo.setRv(sumRv);
		sumVo.setPvRate("100.00%");
		list.set(0, sumVo);
		
		for(int i = 1; i < list.size(); i++){
			vo = list.get(i);
			if (vo == null) continue;
			vo.setPvRate(NumberUtil.percentage(vo.getPv(), sumRv));
			list.set(i, vo);
		}
		
		return list;
	}

	@Override
	public String findHourReport(String startDate, String endDate, Integer labelCount, Integer siteId) {
		List<String> categories = DateTimeUtil.getTimeArea();
		List<SummaryVo> summaryVos = new ArrayList<SummaryVo>();
		List<String> dateArea = DateTimeUtil.getDateArea(startDate, endDate);
		SummaryVo vo = null;
		for (int i = 0; i <= 23; i++){
			String start = "" + i + ":00";
			String end = "" + i + ":59";
			vo = new SummaryVo();
			vo.setName(String.format("%02d", i));
			Long countIp = 0L, countUv = 0L, countPv = 0L, countRv = 0L;
			for (String dateValue : dateArea){
				Date date = DateTimeUtil.getStringToDate(dateValue);
				Date startTime = DateTimeUtil.getStringToTime(start);
				Date endTime = DateTimeUtil.getStringToTime(end);
				
				countIp += visitDAO.findIpCountInHour(date, startTime, endTime, siteId);
				countUv += visitDAO.findUvCountInHour(date, startTime, endTime, siteId);
				countPv += visitDAO.findPvCountInHour(date, startTime, endTime, siteId);
				countRv += visitDAO.findRvCountInHour(date, startTime, endTime, siteId);
			}
			vo.setIp(countIp);
			vo.setUv(countUv);
			vo.setPv(countPv);
			vo.setRv(countRv);
			summaryVos.add(vo);
			
		}
		
		Map<String, Map<String, Long>> dataSet = new LinkedHashMap<String, Map<String, Long>>();
		
		Map<String, Long> mapIp = new LinkedHashMap<String, Long>();
		Map<String, Long> mapUv = new LinkedHashMap<String, Long>();
		Map<String, Long> mapPv = new LinkedHashMap<String, Long>();
		for (SummaryVo summaryVo : summaryVos){
			mapIp.put(summaryVo.getName(), summaryVo.getIp());
			mapUv.put(summaryVo.getName(), summaryVo.getUv());
			mapPv.put(summaryVo.getName(), summaryVo.getPv());
		}
		
		dataSet.put("IP", mapIp);
		dataSet.put("UV", mapUv);
		dataSet.put("PV", mapPv);
		
		return ChartVisitUtil.getLine2DChart(categories, dataSet, labelCount);
	}

	@Override
	public List<InAndExitVo> findEntranceTable(String startDate, String endDate, Integer rows, Integer siteId) {
		List<InAndExitVo>  list = visitDAO.findEntrance(DateTimeUtil.getStringToDate(startDate), DateTimeUtil.getStringToDate(endDate), rows, siteId);
		Long sum = visitDAO.findUrlSumInEntrance(DateTimeUtil.getStringToDate(startDate), DateTimeUtil.getStringToDate(endDate), rows, siteId);
		return conversion(list, sum);
	}

	@Override
	public String findEmtranceTrendReport(String url, String startDate, String endDate, Integer labelCount, Integer siteId) {
		List<String> categories = DateTimeUtil.getDateArea(startDate, endDate);
		Map<String, Map<String, Long>> dataSet = new LinkedHashMap<String, Map<String, Long>>();
		Map<String, Long> dataValue = new LinkedHashMap<String, Long>();
		for (String category : categories){
			Long countPv = visitDAO.findPvCountInEntrance(DateTimeUtil.getStringToDate(category), url, siteId);
			dataValue.put(category, countPv);
		}
		dataSet.put(url, dataValue);
		
		return ChartVisitUtil.getLine2DChart(categories, dataSet, labelCount);
	}

	@Override
	public List<InAndExitVo> findExitTable(String startDate, String endDate, Integer rows, Integer siteId) {
		List<InAndExitVo> list = visitDAO.findExit(DateTimeUtil.getStringToDate(startDate), DateTimeUtil.getStringToDate(endDate), rows, siteId);
		Long sum = visitDAO.findUrlSumInExit(DateTimeUtil.getStringToDate(startDate), DateTimeUtil.getStringToDate(endDate), rows, siteId);
		return conversion(list, sum);
	}

	@Override
	public String findExitTrendReport(String url, String startDate, String endDate, Integer labelCount, Integer siteId) {
		List<String> categories = DateTimeUtil.getDateArea(startDate, endDate);
		Map<String, Map<String, Long>> dataSet = new LinkedHashMap<String, Map<String, Long>>();
		Map<String, Long> dataValue = new LinkedHashMap<String, Long>();
		for (String category : categories){
			Long countPv = visitDAO.findPvCountInExit(DateTimeUtil.getStringToDate(category), url, siteId);
			dataValue.put(category, countPv);
		}
		dataSet.put(url, dataValue);
		
		return ChartVisitUtil.getLine2DChart(categories, dataSet, labelCount);
	}

	@Override
	public List<SummaryVo> findHostTable(String startDate, String endDate, Integer rows, Integer siteId) {
		List<SummaryVo> list = visitDAO.findHost(DateTimeUtil.getStringToDate(startDate), DateTimeUtil.getStringToDate(endDate), rows, siteId);
		Long sum = visitDAO.findPvSumInHost(DateTimeUtil.getStringToDate(startDate), DateTimeUtil.getStringToDate(endDate), rows, siteId);
		if (list == null) return new ArrayList<SummaryVo>(0);
		if (sum == 0) return list;
		SummaryVo vo = null;
		for (int i = 0; i < list.size(); i++){
			vo = list.get(i);
			vo.setPvRate(NumberUtil.percentage(vo.getSumPv().longValue(), sum.longValue()));
			list.set(i, vo);
		}
		return list;
	}

	@Override
	public String findHostTrendReport(String host, String startDate, String endDate, Integer labelCount, Integer siteId) {
		List<String> categories = DateTimeUtil.getDateArea(startDate, endDate);
		Map<String, Map<String, Long>> dataSet = new LinkedHashMap<String, Map<String, Long>>();
		Map<String, Long> dataValue = new LinkedHashMap<String, Long>();
		for (String category : categories){
			Long countPv = visitDAO.findPvCountInHost(DateTimeUtil.getStringToDate(category), host, siteId);
			dataValue.put(category, countPv);
		}
		dataSet.put(host, dataValue);
		
		return ChartVisitUtil.getLine2DChart(categories, dataSet, labelCount);
	}

	@Override
	public String findHostReport(String startDate, String endDate, Integer rows, Integer siteId) {
		List<SummaryVo> list = visitDAO.findHost(DateTimeUtil.getStringToDate(startDate), DateTimeUtil.getStringToDate(endDate), rows, siteId);
		Map<String, Long> dataSet = new LinkedHashMap<String, Long>();
		for (SummaryVo vo : list){
			dataSet.put(vo.getName(), vo.getSumPv());
		}
		return ChartVisitUtil.getPie3DChart(dataSet);
	}

	@Override
	public List<SummaryVo> findCountryTable(String startDate, String endDate, Integer siteId) {
		Date start = DateTimeUtil.getStringToDate(startDate);
		Date end = DateTimeUtil.getStringToDate(endDate);
		List<String> categories = DateTimeUtil.getDateArea(startDate, endDate);
		List<String> countries = visitDAO.findCountryName(start, end, siteId);
		List<SummaryVo> summarys = new ArrayList<SummaryVo>();
		Long sumPv = visitDAO.findPvSumInCountry(start, end, siteId);
		SummaryVo vo = null;
		for (String country : countries){
			vo = new SummaryVo();
			Long countPv = 0L, countUv = 0L, countIp = 0L;
			for (String category : categories){
				Date categoryDate = DateTimeUtil.getStringToDate(category);
				countPv += visitDAO.findPvSumInCountryByCountryName(categoryDate, country, siteId).intValue();
				countUv += visitDAO.findUvCountInDayByCountryName(categoryDate, country, siteId);
				countIp += visitDAO.findIpCountInDayByCountryName(categoryDate, country, siteId);
			}
			vo.setIp(countIp);
			vo.setUv(countUv);
			vo.setPv(countPv);
			vo.setPvRate(NumberUtil.percentage(countPv.longValue(), sumPv.longValue()));
			vo.setName(country);
			
			summarys.add(vo);
		}
		return summarys;
	}

	@Override
	public String findCountryReport(String startDate, String endDate, Integer siteId) {
		List<SummaryVo> list = findCountryTable(startDate, endDate, siteId);
		Map<String, Long> dataSet = new LinkedHashMap<String, Long>();
		for (SummaryVo vo : list){
			dataSet.put(vo.getName(), vo.getPv().longValue());
		}
		return ChartVisitUtil.getPie3DChart(dataSet);
	}

	@Override
	public String findCountryTrendReport(String startDate, String endDate, String country, Integer labelCount, Integer siteId) {
		List<String> categories = DateTimeUtil.getDateArea(startDate, endDate);
		
		Map<String, Map<String, Long>> dataSet = new LinkedHashMap<String, Map<String, Long>>();
		Map<String, Long> dataValuePv = new LinkedHashMap<String, Long>();
		Map<String, Long> dataValueUv = new LinkedHashMap<String, Long>();
		Map<String, Long> dataValueIp = new LinkedHashMap<String, Long>();
		for (String category : categories){
			Long countPv = visitDAO.findPvSumInCountryByCountryName(DateTimeUtil.getStringToDate(category), country, siteId);
			dataValuePv.put(category, countPv);
			Long countUv = visitDAO.findUvCountInDayByCountryName(DateTimeUtil.getStringToDate(category), country, siteId);
			dataValueUv.put(category, countUv);
			Long countIp = visitDAO.findIpCountInDayByCountryName(DateTimeUtil.getStringToDate(category), country, siteId);
			dataValueIp.put(category, countIp);
		}
		dataSet.put("PV", dataValuePv);
		dataSet.put("UV", dataValueUv);
		dataSet.put("IP", dataValueIp);
		
		return ChartVisitUtil.getLine2DChart(categories, dataSet, labelCount);
	}

	@Override
	public String findOnlineReport(String startDate, String endDate, Integer labelCount, Integer siteId) {
		List<String> categories = DateTimeUtil.getTimeArea();
		List<String> dateArea = DateTimeUtil.getDateArea(startDate, endDate);
		
		Map<String, Map<String, Long>> dataSet = new LinkedHashMap<String, Map<String, Long>>();
		
		Map<String, Long> mapFive = new LinkedHashMap<String, Long>();
		Map<String, Long> mapTen = new LinkedHashMap<String, Long>();
		Map<String, Long> mapFifteen = new LinkedHashMap<String, Long>();
		
		for (int i = 0; i <= 23; i++){
			String start = "" + i + ":00";
			String end = "" + i + ":59";
			String hour = String.format("%02d", i);
			Long countFive = 0L, countTen = 0L, countFifteen = 0L;
			for (String dateValue : dateArea){
				Date date = DateTimeUtil.getStringToDate(dateValue);
				Date startTime = DateTimeUtil.getStringToTime(start);
				Date endTime = DateTimeUtil.getStringToTime(end);
				
				List<Long> stickTimes = visitDAO.findOnline(date, startTime, endTime, siteId);
				for (Long stickTime : stickTimes){
					if (stickTime <= 5*60)	countFive = countFive + 1;
					else if (stickTime > 5*60 && stickTime <= 10*60) countTen = countTen + 1;
					else countFifteen = countFifteen + 1;
				}
			}
			mapFive.put(hour, countFive);
			mapTen.put(hour, countTen);
			mapFifteen.put(hour, countFifteen);
		}
		dataSet.put("5分钟", mapFive);
		dataSet.put("10分钟", mapTen);
		dataSet.put("15分钟", mapFifteen);
		
		return ChartVisitUtil.getLine2DChart(categories, dataSet, labelCount);
	}

	@Override
	public List<OnlineVo> findOnlineTable(String startDate, String endDate, Integer siteId) {
		List<OnlineVo> list = new ArrayList<OnlineVo>();
		List<String> dateArea = DateTimeUtil.getDateArea(startDate, endDate);
		
		OnlineVo vo = null;
		for (int i = 0; i <= 23; i++){
			String start = "" + i + ":00";
			String end = "" + i + ":59";
			vo = new OnlineVo();
			vo.setName(start + " —— " + end);
			Integer countFive = 0, countTen = 0, countFifteen = 0;
			for (String dateValue : dateArea){
				Date date = DateTimeUtil.getStringToDate(dateValue);
				Date startTime = DateTimeUtil.getStringToTime(start);
				Date endTime = DateTimeUtil.getStringToTime(end);
				
				List<Long> stickTimes = visitDAO.findOnline(date, startTime, endTime, siteId);
				for (Long stickTime : stickTimes){
					if (stickTime <= 5*60)	countFive = countFive + 1;
					else if (stickTime > 5*60 && stickTime <= 10*60) countTen = countTen + 1;
					else countFifteen = countFifteen + 1;
				}
			}
			vo.setFive(countFive);
			vo.setTen(countTen);
			vo.setFifteen(countFifteen);
			list.add(vo);
		}
		
		return list;
	}

	private Map<String, Long> findSummaryIp(List<String> categories, Integer siteId){
		Map<String, Long> result = new LinkedHashMap<String, Long>();
		for (String category : categories){
			Date date = DateTimeUtil.getStringToDate(category);
			Long value = visitDAO.findIpCountInDay(date, siteId);
			result.put(category, value);
		}
		return result;
	}
	
	private Map<String, Long> findSummaryPv(List<String> categories, Integer siteId){
		Map<String, Long> result = new LinkedHashMap<String, Long>();
		for (String category : categories){
			Date date = DateTimeUtil.getStringToDate(category);
			Long value = visitDAO.findPvCountInDay(date, siteId);
			result.put(category, value);
		}
		return result;
	}

	private Map<String, Long> findSummaryUv(List<String> categories, Integer siteId){
		Map<String, Long> result = new LinkedHashMap<String, Long>();
		for (String category : categories){
			Date date = DateTimeUtil.getStringToDate(category);
			Long value = visitDAO.findUvCountInDay(date, siteId);
			result.put(category, value);
		}
		return result;
	}
	
	private SummaryVo assign(String name, Integer siteId, Date date){
		SummaryVo vo = new SummaryVo();
		vo.setName(name);
		Long countIp = visitDAO.findIpCountInDay(date, siteId);
		vo.setIp(countIp);
		Long countUv = visitDAO.findUvCountInDay(date, siteId);
		vo.setUv(countUv);
		Long countPv = visitDAO.findPvCountInDay(date, siteId);
		vo.setPv(countPv);
		Long countSt = visitDAO.findStickTimeCountInDay(date, siteId);
		Long sumSt = visitDAO.findStickTimeSumInDay(date, siteId);
		Long avgTime = (countSt == 0 ? 0 : sumSt/countSt);
		vo.setAvgTime(Long.toString(avgTime));
		Long countRv = visitDAO.findRvCountInDay(date, siteId);
		vo.setRv(countRv);
		Long count = visitDAO.findAccessCountInDay(date, siteId);
		vo.setRvRate(NumberUtil.percentage(countRv.longValue(), count.longValue()));
		return vo;
	}
	
	private SummaryVo assign(String name, Integer siteId, List<String> dateArea){
		SummaryVo vo = new SummaryVo();
		vo.setName(name);
		Long countIp = 0L, countUv = 0L, countPv = 0L, avgTime = 0L, countRv = 0L, count = 0L;
		for(String dateValue : dateArea){
			Date date = DateTimeUtil.getStringToDate(dateValue);
			countIp += visitDAO.findIpCountInDay(date, siteId);
			countUv += visitDAO.findUvCountInDay(date, siteId);
			countPv += visitDAO.findPvCountInDay(date, siteId);
			Long sumSt = visitDAO.findStickTimeSumInDay(date, siteId);
			Long countSt = visitDAO.findStickTimeCountInDay(date, siteId);
			avgTime = avgTime + (countSt == 0 ? 0 : sumSt / countSt);
			countRv += visitDAO.findRvCountInDay(date, siteId);
			count += visitDAO.findAccessCountInDay(date, siteId);
		}
		vo.setIp(countIp);
		vo.setPv(countPv);
		vo.setUv(countUv);
		vo.setAvgTime(Long.toString(avgTime));
		vo.setRv(countRv);
		vo.setRvRate(NumberUtil.percentage(countRv, count));
		
		return vo;
	}
	
	private List<InAndExitVo> conversion(List<InAndExitVo> list, Long sum){
		if (list == null) return new ArrayList<InAndExitVo>(0);
		if (sum == 0) return list;
		
		InAndExitVo vo = null;
		for (int i = 0; i < list.size(); i++){
			vo = list.get(i);
			vo.setRate(NumberUtil.percentage(vo.getCount(), sum));
			list.set(i, vo);
		}
		return list;
	}

}