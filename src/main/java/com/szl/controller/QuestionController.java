package com.szl.controller;

import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import com.szl.Config;
import com.szl.util.CookieUtil;
import com.szl.util.Filter;
import com.szl.page.Page;
import com.szl.domain.Forward;
import com.szl.domain.Reverse;
import com.szl.page.PageUtil;
import com.szl.service.QuestionSearchService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by zsc on 2017/1/18.
 * Pattern.quote
 * 倒排列表插入两次
 */
@Controller
@SessionAttributes("forwards")
public class QuestionController {

    private HashMap<String, List<Integer>> cache = new HashMap<String, List<Integer>>();

    @Autowired
    private QuestionSearchService questionSearchService;

    @RequestMapping("/")
    public String search() {
        return "searchForm";
    }

//    @RequestMapping("/search")
//    public ModelAndView getQuestions(HttpServletRequest request, @RequestParam(value = "type") String type, @RequestParam(value = "q") String questionStr) {
//        ModelAndView mav;
//        List<Forward> forwards;
//        List<Integer> allIds;
//        List<Integer> everyIds = new ArrayList<Integer>();
//        Long totalCount = -1l;
//        Page page;
//        Map<String, Object> map = new HashMap<String, Object>();
//
//        if (type.equals("question")) {
//            if (cache.containsKey(questionStr)) {
//                allIds = cache.get(questionStr);
//                System.out.println("从缓存里取");
//            } else {
//                allIds = genIds(type, questionStr, questionSearchService.getfQuestionsMap(), questionSearchService.getrQuestionsMap());
//                cache.put(questionStr, allIds);
//                System.out.println("重新计算");
//            }
////            totalCount = questionSearchService.getQPageCounts(allIds);
//            //设置分页对象
//            page = PageUtil.executePage(request, totalCount);
//            for (long i = page.getBeginIndex(); i < (Math.min(page.getEndinIndex(), allIds.size())); i++) {
//                everyIds.add(allIds.get((int) i));
//            }
//            page.setAllIds(allIds);
//            page.setEveryIds(everyIds);
//            map.put("page", page);
//            map.put("request", request);
//            forwards = questionSearchService.selectQByMap(map);
//            mav = new ModelAndView("questionsResult");
//        } else if (type.equals("people")) {
//            if (cache.containsKey(questionStr)) {
//                allIds = cache.get(questionStr);
//                System.out.println("从缓存里取");
//            } else {
//                allIds = genIds(type, questionStr, questionSearchService.getfPeoplesMap(), questionSearchService.getrPeoplesMap());
//                cache.put(questionStr, allIds);
//                System.out.println("重新计算");
//            }
////            totalCount = questionSearchService.getPPageCounts(allIds);
//            //设置分页对象
//            page = PageUtil.executePage(request, totalCount);
//            for (long i = page.getBeginIndex(); i < (Math.min(page.getEndinIndex(), allIds.size())); i++) {
//                everyIds.add(allIds.get((int) i));
//            }
//            page.setAllIds(allIds);
//            page.setEveryIds(everyIds);
//            map.put("page", page);
//            map.put("request", request);
//            forwards = questionSearchService.selectPByMap(map);
//            mav = new ModelAndView("peoplesResult");
//        } else {
//            if (cache.containsKey(questionStr)) {
//                allIds = cache.get(questionStr);
//                System.out.println("从缓存里取");
//            } else {
//                allIds = genIds(type, questionStr, questionSearchService.getfTopicsMap(), questionSearchService.getrTopicsMap());
//                cache.put(questionStr, allIds);
//                System.out.println("重新计算");
//            }
////            totalCount = questionSearchService.getTPageCounts(allIds);
//            //设置分页对象
//            page = PageUtil.executePage(request, totalCount);
//            for (long i = page.getBeginIndex(); i < (Math.min(page.getEndinIndex(), allIds.size())); i++) {
//                everyIds.add(allIds.get((int) i));
//            }
//            page.setAllIds(allIds);
//            page.setEveryIds(everyIds);
//            map.put("page", page);
//            map.put("request", request);
//            forwards = questionSearchService.selectTByMap(map);
//            mav = new ModelAndView("topicsResult");
//        }
//
//        mav.addObject("q", questionStr);
//        mav.addObject("forwards", forwards);
//        return mav;
//    }


    /**
     * 不使用插件，注释mybatis-config代码
     *
     * @param request
     * @param type
     * @param questionStr
     * @return
     * @RequestParam(value="q")中value要和jsp的input的name(只有name，id无所谓)相同，方法为get，post不显示
     */
    @RequestMapping("/search")
    public ModelAndView getQuestions(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = "type") String type, @RequestParam(value = "q") String questionStr) {
        long a = System.currentTimeMillis();
        ModelAndView mav = new ModelAndView("frame");
        List<Forward> forwards;
        List<Forward> sortedForwards;
        List<List<Integer>> list;
        List<Integer> leftID;
        List<Integer> rightID;
        List<Integer> leftPageID;
        List<Integer> rightPageID;
        Long totalCount;
        Page page;

        if (type.equals("question")) {
            //得到全部pageNum
            list = questionSearchService.getAllID(type, questionStr);
//            list = genIds(type, questionStr);
            leftID = list.get(0);
            rightID = list.get(1);
//            System.out.println("allids " + allIds.size() + "\t" + "sortedids " + sortedIds.size());
            if (leftID.size() > 0) {
                //得到当前页面的pageNum
                totalCount = questionSearchService.getQPageCounts(leftID);
                //设置分页对象
                page = PageUtil.executePage(request, totalCount);
                leftPageID = questionSearchService.getLeftPageID(page, leftID);
                rightPageID = questionSearchService.getLRightPageID(page, rightID);
                Cookie cookie = CookieUtil.getCookie(request, "zscNav");
                //首次获取cookie
                if (cookie == null) {
                    cookie = new Cookie("zscNav", "0");
                    cookie.setMaxAge(60);
                    response.addCookie(cookie);
                }
                //重新查询重置cookie
                if (StringUtils.isEmpty(request.getParameter("redirect")) && StringUtils.isEmpty(request.getParameter("pageAction"))) {
                    cookie.setValue("0");
                    cookie.setMaxAge(60);
                    response.addCookie(cookie);
                }
                //判断标签页，第一次cookie为null
                if (cookie == null || cookie.getValue().equals("0")) {
                    forwards = questionSearchService.selectQByPage(leftPageID);
//                    System.out.println("sss3 " + forwards.size());
                    mav.addObject("forwards", forwards);
                    mav.addObject("nav", "0");
                } else {
                    sortedForwards = questionSearchService.selectQByPage(rightPageID);
                    mav.addObject("sortedForwards", sortedForwards);
                    mav.addObject("nav", "1");
                }
            }

        } else if (type.equals("people")) {
            leftID = questionSearchService.getAllID(type, questionStr).get(0);

            if (leftID.size() > 0) {
                totalCount = questionSearchService.getPPageCounts(leftID);
                //设置分页对象
                page = PageUtil.executePage(request, totalCount);
                leftPageID = questionSearchService.getLeftPageID(page, leftID);
                forwards = questionSearchService.selectPByPage(leftPageID);
                mav.addObject("forwards", forwards);
            }

        } else {
            leftID = questionSearchService.getAllID(type, questionStr).get(0);
            if (leftID.size() > 0) {
                totalCount = questionSearchService.getTPageCounts(leftID);
                //设置分页对象
                page = PageUtil.executePage(request, totalCount);
                leftPageID = questionSearchService.getLeftPageID(page, leftID);
                forwards = questionSearchService.selectTByPage(leftPageID);
                mav.addObject("forwards", forwards);
            }
        }
        mav.addObject("idCount", leftID.size());
        mav.addObject("type", type);
        mav.addObject("q", questionStr);
        long b = System.currentTimeMillis();
        System.out.println("time " + (b - a));
        return mav;
    }

    /**
     * setCookie 0
     *
     * @return
     * @RequestParam(value="q")中value要和jsp的input的name(只有name，id无所谓)相同，方法为get，post不显示
     */
    @RequestMapping("/nav0")
    public String nav0(HttpServletRequest request, HttpServletResponse response, RedirectAttributes attributes, @RequestParam(value = "q") String questionStr) {


//        Cookie[] cookies = request.getCookies();
//        if (cookies == null) {
//            System.out.println("没有cookie==============");
//        } else {
//            for (Cookie cookie : cookies) {
//                if (cookie.getName().equals("zscNav")) {
//                    System.out.println("原值为:" + cookie.getValue());
//                    cookie.setValue("0");
////                    cookie.setPath("/");
//                    cookie.setMaxAge(60 * 60);// 设置为60min
//                    response.addCookie(cookie);
//                    break;
//                }
//            }
//        }
        if (CookieUtil.getCookie(request, "zscNav") == null) {
            Cookie cookie = new Cookie("zscNav", "0");
            cookie.setMaxAge(60);// 设置为60min
            response.addCookie(cookie);
        } else {
            Cookie cookie = CookieUtil.getCookie(request, "zscNav");
            cookie.setValue("0");
            cookie.setMaxAge(60);// 设置为60min
            response.addCookie(cookie);
        }


        attributes.addAttribute("type", "question");
        attributes.addAttribute("q", questionStr);
        attributes.addAttribute("redirect", "0");

        return "redirect:/search";
    }

    @RequestMapping("/nav1")
    public String nav1(HttpServletRequest request, HttpServletResponse response, RedirectAttributes attributes, @RequestParam(value = "q") String questionStr) {
//        Cookie[] cookies = request.getCookies();
//        if (cookies == null) {
//            System.out.println("没有cookie==============");
//        } else {
//            for (Cookie cookie : cookies) {
//                if (cookie.getName().equals("zscNav")) {
//                    System.out.println("原值为:" + cookie.getValue());
//                    cookie.setValue("1");
////                    cookie.setPath("/");
//                    cookie.setMaxAge(60);// 设置为60min
//                    response.addCookie(cookie);
//                    break;
//                }
//            }
//        }
        if (CookieUtil.getCookie(request, "zscNav") == null) {
            Cookie cookie = new Cookie("zscNav", "1");
            cookie.setMaxAge(60);
            response.addCookie(cookie);
        } else {
            Cookie cookie = CookieUtil.getCookie(request, "zscNav");
            cookie.setValue("1");
            cookie.setMaxAge(60);
            response.addCookie(cookie);
        }
        attributes.addAttribute("type", "question");
        attributes.addAttribute("q", questionStr);
        attributes.addAttribute("redirect", "1");
        return "redirect:/search";
    }


//    private List<List<Integer>> genIds(String type, String str) {
//        System.out.println(str);
//        //得到按序排列的关键字集合
//        List<Term> terms = Filter.accept(StandardTokenizer.segment(str));
//        System.out.println("分词结果 " + terms.toString());
//
//        List<List<Integer>> list = new ArrayList<List<Integer>>();
//        List<Integer> defaultID = new ArrayList<Integer>();
//        List<Integer> TFIDFID = new ArrayList<Integer>();
//        List<Integer> qualityID = new ArrayList<Integer>();
//        List<Reverse> keyWords = new ArrayList<Reverse>();
//        Reverse key;
//        for (Term term : terms) {
//            if (type.equals("question")) {
//                key = questionSearchService.getQUrls(term.word);
//            } else if (type.equals("people")) {
//                key = questionSearchService.getPUrls(term.word);
//            } else {
//                key = questionSearchService.getTUrls(term.word);
//            }
//
//
//            if (key != null) {
//                keyWords.add(key);
//            }
//        }
//
//        //此处有区别，使用type区分(对people和topic进行分词，全部统一)@Deprecated
////        if (type.equals("question")) {
////            for (Term term : terms) {
////                if (rQuestionsMap.containsKey(term.word)) {
////                    keyWords.add(rQuestionsMap.get(term.word));
////                }
////            }
////        } else {
////            for (Term term : terms) {
////                for (Map.Entry<String, Reverse> entry : rQuestionsMap.entrySet()) {
////                    if (entry.getKey().contains(term.word)) {
////                        keyWords.add(entry.getValue());
////                    }
////                }
////            }
////        }
//
//        Collections.sort(keyWords);//按IDF大小排序
//
//        //得到按序排列的url集合，只要string
//        List<String> TFIDFUrls = new ArrayList<String>();
//        List<String> defaultUrls = new ArrayList<String>();
//        List<String> qualityUrls = new ArrayList<String>();
//
//        for (Reverse reverse : keyWords) {
//            TFIDFUrls.add(reverse.getTFIDF());
//            defaultUrls.add(reverse.getPageID());
//            qualityUrls.add(reverse.getQualityAndPID());
//        }
//
//        //得到最终排序
//        Set<String> defaultSet = new LinkedHashSet<String>();//保证按顺序且不重复，按关联度&关注度
//        Set<String> TFIDFSet = new LinkedHashSet<String>();//按TFIDF&关注度
//        Set<String> qualitySet = new HashSet<String>();//按关注度
//
//
//        List<String> TFIDFTemp = new ArrayList<String>();
//        List<String> defaultTemp = new ArrayList<String>();
//        List<String> qualityTemp = new ArrayList<String>();
//        int len = defaultUrls.size();
//        if (type.equals("question")) {
//            for (int i = len; i != 0; i--) {
////                genQInSequence(defaultSet, TFIDFSet, qualitySet, defaultUrls, TFIDFUrls, qualityUrls, 0, i, TFIDFTemp, defaultTemp, qualityTemp);
//                genQInSequence(defaultSet, TFIDFSet, qualitySet, defaultUrls, TFIDFUrls, qualityUrls, len, i);//非递归
//            }
////            genQuestionList(defaultSet, TFIDFSet, qualitySet, defaultUrls, TFIDFUrls, qualityUrls, len);//相关性↑
//        } else {
//            for (int i = len; i != 0; i--) {
////                genInSequence(defaultSet, defaultUrls, 0, i, defaultTemp);//递归
//                genInSequence(defaultSet, defaultUrls, len, i);//非递归
//
//            }
//        }
//
//        //显示结果
//        //默认
//        for (String url : defaultSet) {
//            defaultID.add(Integer.parseInt(url));
//        }
//
//        //TFIDF
//        for (String url : TFIDFSet) {
//            TFIDFID.add(Integer.parseInt(url));
//        }
//
//        //按关注度
//        List<String> quality = new ArrayList<String>(qualitySet);
//
//        Collections.sort(quality, new Comparator<String>() {
//            @Override
//            public int compare(String o1, String o2) {
//                return -o1.compareTo(o2);
//            }
//        });
//        for (String url : quality) {
//            qualityID.add(Integer.parseInt(url.split(",")[1]));
//        }
//
//        if (type.equals("question")) {
//            list.add(TFIDFID);
//            list.add(defaultID);
//        } else {
//            list.add(defaultID);
//        }
//        return list;
//    }
//
//    //返回List<Forward>，@Deprecated
//    /*private List<Forward> genUrls(String type, String str, Map<String, Forward> fQuestionsMap, Map<String, Reverse> rQuestionsMap) {
//        //得到按序排列的关键字集合
//        List<Term> terms = Filter.accept(HanLP.segment(str));
//        for (int i = 0; i < terms.size(); i++) {
//            System.out.println("分词结果 " + terms.get(i).word);
//        }
//        List<Forward> forwards = new ArrayList<Forward>();
//        List<Reverse> keyWords = new ArrayList<Reverse>();
//        //此处有区别，使用type区分
//        if (type.equals("question")) {
//            for (Term term : terms) {
//                if (rQuestionsMap.containsKey(term.word)) {
//                    keyWords.add(rQuestionsMap.get(term.word));
//                }
//            }
//        } else {
//            for (Term term : terms) {
//                for (Map.Entry<String, Reverse> entry : rQuestionsMap.entrySet()) {
//                    if (entry.getKey().contains(term.word)) {
//                        keyWords.add(entry.getValue());
//                    }
//                }
//            }
//        }
//
//        Collections.sort(keyWords);//按IDF大小排序
//
//        //得到按序排列的url集合，只要string
//        List<String> sortedUrls = new ArrayList<String>();
//        for (Reverse reverse : keyWords) {
//            sortedUrls.add(reverse.getPageID());
//        }
//
//        //得到最终排序
//        Set<String> urls = new LinkedHashSet<String>();//保证按顺序且不重复
//        List<String> temp = new ArrayList<String>();
//        int len = sortedUrls.size() + 1;
//        for (int i = len - 1; i != 0; i--) {
//            genInSequence(urls, sortedUrls, 0, i, temp);
//        }
//
//        //显示结果
////        System.out.println("最终url: " + urls);
//        for (String num : urls) {
//            if (fQuestionsMap.containsKey(num)) {
//                forwards.add(fQuestionsMap.get(num));
//            }
//        }
////        for (int i = 0; i < forwards.size(); i++) {
////            System.out.println("最终结果  " + forwards.get(i).getId());
////        }
//        return forwards;
//    }*/
//
//    /**
//     * 只取1234,123,12,1
//     * @param defaultSet
//     * @param TFIDFSet
//     * @param qualitySet
//     * @param defaultUrls
//     * @param TFIDFUrls
//     * @param qualityUrls
//     * @param len
//     */
//    private void genQuestionList(Set<String> defaultSet, Set<String> TFIDFSet, Set<String> qualitySet,
//                                 List<String> defaultUrls, List<String> TFIDFUrls, List<String> qualityUrls, int len) {
//        List<List<String>> defaultList = new ArrayList<List<String>>();
//        List<List<String>> qualityList = new ArrayList<List<String>>();
//        for (int i = 1; i <= len; i++) {
//            if (defaultList.size() == 0) {
//                for (int j = 0; j < i; j++) {
//                    List<String> defaultResult = new ArrayList<String>();
//                    List<String> qualityResult = new ArrayList<String>();
//
//                    defaultResult.addAll(Arrays.asList(defaultUrls.get(0).split(Pattern.quote(Config.DELIMITER))));
//                    qualityResult.addAll(Arrays.asList(qualityUrls.get(0).split(Pattern.quote(Config.DELIMITER))));
//                    for (int k = 1; k < i; k++) {
//                        defaultResult.retainAll(Arrays.asList(defaultUrls.get(k).split(Pattern.quote(Config.DELIMITER))));
//                        qualityResult.retainAll(Arrays.asList(qualityUrls.get(k).split(Pattern.quote(Config.DELIMITER))));
//                    }
//                    defaultList.add(defaultResult);
//                    qualityList.add(qualityResult);
//                }
//            } else {
//                List<String> defaultResult = new ArrayList<String>(defaultList.get(defaultList.size() - 1));
//                defaultResult.retainAll(Arrays.asList(defaultUrls.get(i - 1).split(Pattern.quote(Config.DELIMITER))));
//                defaultList.add(defaultResult);
//                List<String> qualityResult = new ArrayList<String>(qualityList.get(qualityList.size() - 1));
//                qualityResult.retainAll(Arrays.asList(qualityUrls.get(i - 1).split(Pattern.quote(Config.DELIMITER))));
//                qualityList.add(qualityResult);
//            }
//        }
//        for (int i = defaultList.size() - 1; i >= 0; i--) {
//            defaultSet.addAll(defaultList.get(i));
//            qualitySet.addAll(qualityList.get(i));
//        }
//
//    }
//
//
//    /**
//     * 非递归begin
//     */
//    //获取长度为len的组合数C(arrLen,len)的个数
//    private static int getCountOfCombinations(int arrLen, int len) {
//        int m = 1;
//        for (int i = 0; i < len; i++) {
//            m *= arrLen - i;
//        }
//        int n = 1;
//        for (int i = len; i > 1; i--) {
//            n *= i;
//        }
//        return m / n;
//    }
//
//
//    private void genInSequence(Set<String> defaultSet, List<String> defaultUrls, int len, int subLen) {
//        List<String> tempList = new ArrayList<String>();
//        int[] array = new int[len];
//        for (int j = 0; j < len; j++) {
//            if (j < subLen) {
//                array[j] = 1;
//            } else {
//                array[j] = 0;
//            }
//        }
//
//
//        //得到C(len,subLen)
//        for (int j = 0; j < array.length; j++) {
//            if (array[j] == 1) {
//                tempList.add(defaultUrls.get(j));
//            }
//        }
//        List<String> result = new ArrayList<String>();
//        result.addAll(Arrays.asList(tempList.get(0).split(Pattern.quote(Config.DELIMITER))));
//        for (int i = 1; i < tempList.size(); i++) {
//            result.retainAll(Arrays.asList(tempList.get(i).split(Pattern.quote(Config.DELIMITER))));
//        }
//        defaultSet.addAll(result);
//
//
//
//        int n = getCountOfCombinations(len, subLen);//得到C(len,i)组合数个数
//        for (int j = 1; j < n; j++) {
//            for (int k = array.length - 1; k > 0; k--) {
//                if (array[k] == 0 && array[k - 1] == 1) {
//                    array[k] = 1;
//                    array[k - 1] = 0;
//                    int start = k;
//                    int end = len - 1;
//                    while (true) {
//                        while (array[start] == 1) {
//                            start++;
//                            if (start >= len)
//                                break;
//                        }
//                        while (array[end] == 0) {
//                            end--;
//                            if (end < k)
//                                break;
//                        }
//
//                        if (start < end) {
//                            int temp = array[end];
//                            array[end] = array[start];
//                            array[start] = temp;
//                        } else {
//                            break;
//                        }
//                    }
//                    break;
//                }
//            }
//            //得到一条组合
//            tempList = new ArrayList<String>();
//            for (int k = 0; k < array.length; k++) {
//                if (array[k] == 1) {
//                    tempList.add(defaultUrls.get(k));
//                }
//            }
//            result = new ArrayList<String>();
//            result.addAll(Arrays.asList(tempList.get(0).split(Pattern.quote(Config.DELIMITER))));
//            for (int i = 1; i < tempList.size(); i++) {
//                result.retainAll(Arrays.asList(tempList.get(i).split(Pattern.quote(Config.DELIMITER))));
//            }
//            defaultSet.addAll(result);
//        }
//    }
//
//
//    private void genQInSequence(Set<String> defaultSet, Set<String> TFIDFSet, Set<String> qualitySet, List<String> defaultUrls,
//                                List<String> TFIDFUrls, List<String> qualityUrls, int len, int subLen) {
//        List<String> defaultTemp = new ArrayList<String>();
//        List<String> TFIDFTemp = new ArrayList<String>();
//        List<String> qualityTemp = new ArrayList<String>();
//
//        int[] array = new int[len];
//        for (int j = 0; j < len; j++) {
//            if (j < subLen) {
//                array[j] = 1;
//            } else {
//                array[j] = 0;
//            }
//        }
//
//        //得到C(len,subLen)
//        for (int j = 0; j < array.length; j++) {
//            if (array[j] == 1) {
//                defaultTemp.add(defaultUrls.get(j));
//                TFIDFTemp.add(TFIDFUrls.get(j));
//                qualityTemp.add(qualityUrls.get(j));
//            }
//        }
//        List<String> TFIDFResult = new ArrayList<String>();
//        List<String> tempList = new ArrayList<String>();
//        List<String> defaultResult = new ArrayList<String>();
//        List<String> qualityResult = new ArrayList<String>();
//        defaultResult.addAll(Arrays.asList(defaultTemp.get(0).split(Pattern.quote(Config.DELIMITER))));
//        qualityResult.addAll(Arrays.asList(qualityTemp.get(0).split(Pattern.quote(Config.DELIMITER))));
//
//        HashMap<String, String> resultMap = new HashMap<String, String>();
//        for (String str : Arrays.asList(TFIDFTemp.get(0).split(Pattern.quote(Config.DELIMITER)))) {
//            resultMap.put(str.split(",")[0], str.split(",")[1]);
//        }
//        if (defaultTemp.size() == 1) {
////                ArrayList<String> list = new ArrayList<String>();
//            for (Map.Entry<String, String> entry : resultMap.entrySet()) {
//                tempList.add(new StringBuilder().append(entry.getValue()).append(",").append(entry.getKey()).toString());
//            }
//            Collections.sort(tempList, new Comparator<String>() {
//                @Override
//                public int compare(String str1, String str2) {
//                    return -str1.compareTo(str2);
//                }
//            });
//        }
//        for (int i = 1; i < defaultTemp.size(); i++) {
//            HashMap<String, String> resultMap2 = new HashMap<String, String>();
//            for (String str : Arrays.asList(TFIDFTemp.get(i).split(Pattern.quote(Config.DELIMITER)))) {
//                resultMap2.put(str.split(",")[0], str.split(",")[1]);
//            }
//            resultMap = intersect(resultMap, resultMap2);
//            defaultResult.retainAll(Arrays.asList(defaultTemp.get(i).split(Pattern.quote(Config.DELIMITER))));
//            qualityResult.retainAll(Arrays.asList(qualityTemp.get(i).split(Pattern.quote(Config.DELIMITER))));
//        }
//
//        for (Map.Entry<String, String> entry : resultMap.entrySet()) {
//            //TFIDF的和，PID
//            tempList.add(new StringBuilder().append(entry.getValue()).append(",").append(entry.getKey()).toString());
//        }
//        Collections.sort(tempList, new Comparator<String>() {
//            @Override
//            public int compare(String str1, String str2) {
//                return -str1.compareTo(str2);
//            }
//        });
//        for (String str : tempList) {
//            TFIDFResult.add(str.split(",")[1]);
//        }
//        TFIDFSet.addAll(TFIDFResult);
//        defaultSet.addAll(defaultResult);
//        qualitySet.addAll(qualityResult);
//
//
//
//        int n = getCountOfCombinations(len, subLen);//得到C(len,i)组合数个数
//        for (int j = 1; j < n; j++) {
//            for (int k = array.length - 1; k > 0; k--) {
//                if (array[k] == 0 && array[k - 1] == 1) {
//                    array[k] = 1;
//                    array[k - 1] = 0;
//                    int start = k;
//                    int end = len - 1;
//                    while (true) {
//                        while (array[start] == 1) {
//                            start++;
//                            if (start >= len)
//                                break;
//                        }
//                        while (array[end] == 0) {
//                            end--;
//                            if (end < k)
//                                break;
//                        }
//
//                        if (start < end) {
//                            int temp = array[end];
//                            array[end] = array[start];
//                            array[start] = temp;
//                        } else {
//                            break;
//                        }
//                    }
//                    break;
//                }
//            }
//
//
//            //得到一条组合
//            defaultTemp = new ArrayList<String>();
//            TFIDFTemp = new ArrayList<String>();
//            qualityTemp = new ArrayList<String>();
//            for (int k = 0; k < array.length; k++) {
//                if (array[k] == 1) {
//                    defaultTemp.add(defaultUrls.get(k));
//                    TFIDFTemp.add(TFIDFUrls.get(k));
//                    qualityTemp.add(qualityUrls.get(k));
//                }
//            }
//            TFIDFResult = new ArrayList<String>();
//            tempList = new ArrayList<String>();
//            defaultResult = new ArrayList<String>();
//            qualityResult = new ArrayList<String>();
//            defaultResult.addAll(Arrays.asList(defaultTemp.get(0).split(Pattern.quote(Config.DELIMITER))));
//            qualityResult.addAll(Arrays.asList(qualityTemp.get(0).split(Pattern.quote(Config.DELIMITER))));
//
//            resultMap = new HashMap<String, String>();
//            for (String str : Arrays.asList(TFIDFTemp.get(0).split(Pattern.quote(Config.DELIMITER)))) {
//                resultMap.put(str.split(",")[0], str.split(",")[1]);
//            }
//            if (defaultTemp.size() == 1) {
//                for (Map.Entry<String, String> entry : resultMap.entrySet()) {
//                    tempList.add(new StringBuilder().append(entry.getValue()).append(",").append(entry.getKey()).toString());
//                }
//                Collections.sort(tempList, new Comparator<String>() {
//                    @Override
//                    public int compare(String str1, String str2) {
//                        return -str1.compareTo(str2);
//                    }
//                });
//            }
//            for (int i = 1; i < defaultTemp.size(); i++) {
//                HashMap<String, String> resultMap2 = new HashMap<String, String>();
//                for (String str : Arrays.asList(TFIDFTemp.get(i).split(Pattern.quote(Config.DELIMITER)))) {
//                    resultMap2.put(str.split(",")[0], str.split(",")[1]);
//                }
//                resultMap = intersect(resultMap, resultMap2);
//                defaultResult.retainAll(Arrays.asList(defaultTemp.get(i).split(Pattern.quote(Config.DELIMITER))));
//                qualityResult.retainAll(Arrays.asList(qualityTemp.get(i).split(Pattern.quote(Config.DELIMITER))));
//            }
//
//            for (Map.Entry<String, String> entry : resultMap.entrySet()) {
//                //TFIDF的和，PID
//                tempList.add(new StringBuilder().append(entry.getValue()).append(",").append(entry.getKey()).toString());
//            }
//            Collections.sort(tempList, new Comparator<String>() {
//                @Override
//                public int compare(String str1, String str2) {
//                    return -str1.compareTo(str2);
//                }
//            });
//            for (String str : tempList) {
//                TFIDFResult.add(str.split(",")[1]);
//            }
//            TFIDFSet.addAll(TFIDFResult);
//            defaultSet.addAll(defaultResult);
//            qualitySet.addAll(qualityResult);
//        }
//    }
//    /**
//     * 非递归end
//     */
//
//
//    /**
//     * 递归begin
//     */
//
//    //非问题递归获取全组合，并返回求交集后的结果
//    private void genInSequence(Set<String> defaultSet, List<String> defaultUrls, int start, int len, List<String> temp) {//len为组合的长度
//        if (len == 0) {
//            List<String> result = new ArrayList<String>();
//            result.addAll(Arrays.asList(temp.get(0).split(Pattern.quote(Config.DELIMITER))));
//            for (int i = 1; i < temp.size(); i++) {
//                result.retainAll(Arrays.asList(temp.get(i).split(Pattern.quote(Config.DELIMITER))));
//            }
//            defaultSet.addAll(result);
//            return;
//        }
//        if (start == defaultUrls.size()) {
//            return;
//        }
//        temp.add(defaultUrls.get(start));
//        genInSequence(defaultSet, defaultUrls, start + 1, len - 1, temp);
//        temp.remove(temp.size() - 1);
//        genInSequence(defaultSet, defaultUrls, start + 1, len, temp);
//    }
//
//    //问题递归获取全组合，并返回求交集后的结果
//    private void genQInSequence(Set<String> defaultSet, Set<String> TFIDFSet, Set<String> qualitySet, List<String> defaultUrls,
//                                List<String> TFIDFUrls, List<String> qualityUrls, int start, int len,
//                                List<String> TFIDFTemp, List<String> defaultTemp, List<String> qualityTemp) {//len为组合的长度
//        if (len == 0) {
//            List<String> TFIDFResult = new ArrayList<String>();
//            List<String> tempList = new ArrayList<String>();
//            List<String> defaultResult = new ArrayList<String>();
//            List<String> qualityResult = new ArrayList<String>();
//            defaultResult.addAll(Arrays.asList(defaultTemp.get(0).split(Pattern.quote(Config.DELIMITER))));
//            qualityResult.addAll(Arrays.asList(qualityTemp.get(0).split(Pattern.quote(Config.DELIMITER))));
//
//            HashMap<String, String> resultMap = new HashMap<String, String>();
//            for (String str : Arrays.asList(TFIDFTemp.get(0).split(Pattern.quote(Config.DELIMITER)))) {
//                resultMap.put(str.split(",")[0], str.split(",")[1]);
//            }
//            if (defaultTemp.size() == 1) {
//                for (Map.Entry<String, String> entry : resultMap.entrySet()) {
//                    tempList.add(new StringBuilder().append(entry.getValue()).append(",").append(entry.getKey()).toString());
//                }
//                Collections.sort(tempList, new Comparator<String>() {
//                    @Override
//                    public int compare(String str1, String str2) {
//                        return -str1.compareTo(str2);
//                    }
//                });
//            }
//            for (int i = 1; i < defaultTemp.size(); i++) {
//                HashMap<String, String> resultMap2 = new HashMap<String, String>();
//                for (String str : Arrays.asList(TFIDFTemp.get(i).split(Pattern.quote(Config.DELIMITER)))) {
//                    resultMap2.put(str.split(",")[0], str.split(",")[1]);
//                }
//                resultMap = intersect(resultMap, resultMap2);
//                defaultResult.retainAll(Arrays.asList(defaultTemp.get(i).split(Pattern.quote(Config.DELIMITER))));
//                qualityResult.retainAll(Arrays.asList(qualityTemp.get(i).split(Pattern.quote(Config.DELIMITER))));
//            }
//
//            for (Map.Entry<String, String> entry : resultMap.entrySet()) {
//                //TFIDF的和，PID
//                tempList.add(new StringBuilder().append(entry.getValue()).append(",").append(entry.getKey()).toString());
//            }
//            Collections.sort(tempList, new Comparator<String>() {
//                @Override
//                public int compare(String str1, String str2) {
//                    return -str1.compareTo(str2);
//                }
//            });
//            for (String str : tempList) {
//                TFIDFResult.add(str.split(",")[1]);
//            }
//            TFIDFSet.addAll(TFIDFResult);
//            defaultSet.addAll(defaultResult);
//            qualitySet.addAll(qualityResult);
//            return;
//        }
//        if (start == defaultUrls.size()) {
//            return;
//        }
//        TFIDFTemp.add(TFIDFUrls.get(start));
//        defaultTemp.add(defaultUrls.get(start));
//        qualityTemp.add(qualityUrls.get(start));
//        genQInSequence(defaultSet, TFIDFSet, qualitySet, defaultUrls,TFIDFUrls, qualityUrls, start + 1, len - 1, TFIDFTemp, defaultTemp, qualityTemp);
//        TFIDFTemp.remove(TFIDFTemp.size() - 1);
//        defaultTemp.remove(defaultTemp.size() - 1);
//        qualityTemp.remove(qualityTemp.size() - 1);
//        genQInSequence(defaultSet, TFIDFSet, qualitySet, defaultUrls, TFIDFUrls, qualityUrls, start + 1, len, TFIDFTemp, defaultTemp, qualityTemp);
//    }
//
//    private HashMap<String, String> intersect(HashMap<String, String> hashMap1, HashMap<String, String> hashMap2) {
//        HashMap<String, String> hashMap = new HashMap<String, String>();
//        if (hashMap1.size() > hashMap2.size()) {
//            for (Map.Entry<String, String> entry : hashMap1.entrySet()) {
//                if (hashMap2.containsKey(entry.getKey())) {
//                    //TFIDF的和，PID
//                    hashMap.put(entry.getKey(), new StringBuilder().append(String.valueOf(Double.valueOf(entry.getValue()) +
//                            Double.valueOf(hashMap2.get(entry.getKey())))).toString());
//                }
//            }
//        } else {
//            for (Map.Entry<String, String> entry : hashMap2.entrySet()) {
//                if (hashMap1.containsKey(entry.getKey())) {
//                    //TFIDF的和，PID
//                    hashMap.put(entry.getKey(), new StringBuilder().append(String.valueOf(Double.valueOf(entry.getValue()) +
//                            Double.valueOf(hashMap1.get(entry.getKey())))).toString());
//                }
//            }
//        }
//        return hashMap;
//    }
//    /**
//     * 递归end
//     */

    /*class TF_IDF implements Comparable<TF_IDF> {
        private double TF;
        private double IDF;
        private String url;
        private int quality;

        public String getUrl() {
            return url;
        }

        public int getQuality() {
            return quality;
        }

        TF_IDF(String str) {
            this.url = str.split(",")[0];
            this.TF = Double.parseDouble(str.split(",")[1]);
            this.IDF = Double.parseDouble(str.split(",")[2]);
            this.quality = Integer.valueOf(str.split(",")[3]);
        }

        @Override
        public String toString() {
            return url.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return url.equals(((TF_IDF) obj).getUrl());
        }

        @Override
        public int hashCode() {
            return url.hashCode();
        }

        public boolean equal(double a, double b) {
            if ((a - b > -0.000001) && (a - b) < 0.000001)
                return true;
            else
                return false;
        }

        //升序！！！
        @Override
        public int compareTo(TF_IDF object) {
            double str1 = TF * IDF;
            double str2 = object.TF * object.IDF;
            if (equal(str1, str2)) {
                if (quality < object.quality) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                if (str1 < str2) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
    }*/

    /*class QualitySort implements Comparable<QualitySort> {
        private String url;
        private int quality;

        public String getUrl() {
            return url;
        }

        public int getQuality() {
            return quality;
        }

        QualitySort(String url, int quality) {
            this.url = url;
            this.quality = quality;
        }

        @Override
        public String toString() {
            return url.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return url.equals(((QualitySort) obj).getUrl());
        }

        @Override
        public int hashCode() {
            return url.hashCode();
        }

        //升序！！！
        @Override
        public int compareTo(QualitySort object) {
            if (quality < object.quality) {
                return 1;
            } else {
                return -1;
            }
        }
    }*/
}
