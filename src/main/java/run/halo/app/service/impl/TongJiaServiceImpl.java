package run.halo.app.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import run.halo.app.model.entity.Post;
import run.halo.app.model.enums.PostStatus;
import run.halo.app.model.params.PostMetaParam;
import run.halo.app.model.params.PostParam;
import run.halo.app.model.vo.PostDetailVO;
import run.halo.app.service.PostService;
import run.halo.app.service.TongJiaService;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class TongJiaServiceImpl implements TongJiaService {

    @Autowired
    private PostService postService;

    /**
     * 获取长江现货铜价接口API
     * https://www.ccmn.cn/historyprice/cjxh_1/
     * https://m.ccmn.cn/mhangqing/
     */
    public static final String URL_CJXH_API = "https://www.ccmn.cn/historyprice/getCorpStmarketPriceList";
    public static final String MARKETVMID_CJXH = "40288092327140f601327141c0560001";

    public static final String URL_LDQH_API = "https://www.ccmn.cn/historyprice/getCorpStmarketPriceList";
    public static final String MARKETVMID_LDQH = "4028809232715753013271616bd20009";
    public static final String URL_SHYS_API = "https://www.ccmn.cn/historyprice/getCorpStmarketPriceList";
    /**
     * 上海有色
     */
    public static final String MARKETVMID_SHYS = "40288092327143740132714494460002";

    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String YYYY_MM_DD_CHINA = "yyyy年MM月dd日";

    private static final int timeout = 1000;


    @Override
    public boolean autoPubArticle() {
        StringBuffer sb_desc = new StringBuffer();
        StringBuffer sb_price = new StringBuffer("\n" +
                "### <a href=\"http://www.chinazhaolan.com\" target=\"_blant\">找缆网</a>汇总有色金属行情价格：\n" +
                "\n");
        PostParam param = new PostParam();
        //调用api获取长江现货铜价
        JSONObject jsonObject_cj = getChangJiang();
        //调用api获取伦敦期货
        JSONObject jsonObject_ld = getLunTong();
        //调用api获取上海有色
        JSONObject jsonObject_sh = getShangHai();
        String nowDate = date2string(new Date(), YYYY_MM_DD_CHINA);
        //长江现铜
        try {
            if (null != jsonObject_cj && jsonObject_cj.get("success").equals(true)) {
                String nowDate1 = date2string(new Date(), YYYY_MM_DD_CHINA);
                // 解析接口返回接口 ，抽取有用数据
                Map<String, Object> params_cj = getChangJiangStringObjectMap(sb_price, sb_desc, nowDate1, jsonObject_cj);
                //如果接口返回数据不是当天的数据 则尝试获取伦铜
                //            if (!date2string(new Date()).equals(params_cj.get("publishDate"))) {
                if (true) {
                    log.error("长江现货铜价接口返回数据非当天的:" + params_cj.get("publishDate"));
                    //获取伦敦期货
                    sb_desc = new StringBuffer();
                    sb_price = new StringBuffer("\n" +
                            "### <a href=\"http://www.chinazhaolan.com\" target=\"_blant\">找缆网</a>汇总有色金属行情价格：\n" +
                            "\n");
                    // 解析接口返回接口 ，抽取有用数据
                    if (null != jsonObject_ld && jsonObject_ld.get("success").equals(true)) {
                        Map<String, Object> params_ld = getLunDunStringObjectMap(sb_price, sb_desc, nowDate1, jsonObject_ld);

                        String templateName = "找缆网：铜价" + nowDate + "！" + params_cj.get("titleZhangDie");
                        param.setTitle(templateName);
                        //别名
                        //TODO 删除别名
                        param.setSlug(templateName + System.currentTimeMillis());
                        //如果接口返回数据不是当天的数据 则不继续
                        if (!date2string(new Date()).equals(params_ld.get("publishDate"))) {
                            log.error("伦敦期货铜价接口返回数据非当天的:" + params_ld.get("publishDate"));
                            return false;
                        }
                    }
                } else {
                    //长江现货 当天数据
                    String templateName = "找缆网：铜价" + nowDate + "！" + params_cj.get("titleZhangDie");
                    param.setTitle(templateName);
                    //别名
                    //TODO 删除别名
                    param.setSlug(templateName + System.currentTimeMillis());
                    // 解析接口返回接口 ，抽取有用数据
                    if (null != jsonObject_ld && jsonObject_ld.get("success").equals(true)) {
                        Map<String, Object> params_ld = getLunDunStringObjectMap(sb_price, null, nowDate1, jsonObject_ld);
                    }
                    if (null != jsonObject_sh && jsonObject_sh.get("success").equals(true)) {
                        Map<String, Object> params_sh = getShangHaiStringObjectMap(sb_price, null, nowDate1, jsonObject_sh);
                    }

                }
            } else {
                log.error("---获取铜价接口，返回异常--：" + jsonObject_cj.toString());
                //获取伦铜
                return false;
            }
        } catch (Exception e) {
            log.error("=======铜价自动发文章失败=====", e.getMessage());
            return false;
        }
        sb_price.append("\n" +
                "#### 铜价趋势图\n" +
                "\n" +
                "![30天铜价趋势图](http://hq.cu168.com/cn/basemetals/past_charts/cp0030_cn_250x155.gif)\n" +
                "\n" +
                "![半年铜价趋势图](http://hq.cu168.com/cn/basemetals/past_charts/cp0182_cn_250x155.gif)\n" +
                "\n" +
                "--- \n" +
                "* 关注公众号\n" +
                "\n" +
                "* 公众号后台回复【报价】电线电缆实时报价\n" +
                "\n" +
                "* 公众号后台回复【铜价】、【1】实时查看今日铜价\n" +
                "\n" +
                "* 公众号后台回复【趋势】、【2】查看更多时间段铜价趋势图\n" +
                "\n" +
                "* <a href=\"http://www.chinazhaolan.com\" target=\"_blant\">买电缆，上找缆网</a>\n" +
                "\n" +
                "![0.jpg](https://zhaolan.wang/upload/2020/04/0-ef1ffa6129234bdfa3a50c253ffdc596.jpg)\n");
        param.setOriginalContent(sb_price.toString());
        param.setSummary(sb_desc.toString());
        param.setStatus(PostStatus.PUBLISHED);
        //封面图链接
        param.setThumbnail("https://zhaolan.wang/upload/2020/04/%E4%BB%8A%E6%97%A5%E9%93%9C%E4%BB%B7-db97a3d3afa94b1e933ccee1e1028d66.jpg");
        Set<Integer> tagIds = new HashSet<>();
        tagIds.add(4);
        param.setTagIds(tagIds);
        Set<Integer> categoryIds = new HashSet<>();
        categoryIds.add(3);
        param.setCategoryIds(categoryIds);
        Set<PostMetaParam> metas = new HashSet<>();
        param.setMetas(metas);
        param.setMetaKeywords("电线,电缆,找缆网,铜价,电缆平台,国标电缆,平价电缆,找电缆网,电缆报价");
        param.setMetaDescription("买卖电缆,上找缆网 电线,电缆,找缆网,铜价,电缆平台,国标电缆,平价电缆,找电缆网,电缆报价");

        //转换保存
        Post post = param.convertTo();
        PostDetailVO vo = postService.createBy(post, param.getTagIds(), param.getCategoryIds(), param.getPostMetas(), false);
        if (null != vo) {
            return true;
        }
        return false;
    }

    @Nullable
    private JSONObject getShangHai() {
        HashMap<String, Object> paramMap_SHYS = new HashMap<>();
        paramMap_SHYS.put("marketVmid", MARKETVMID_SHYS);
        String result_sh = HttpUtil.post(URL_SHYS_API, paramMap_SHYS, timeout);
        JSONObject jsonObject_sh = null;
        try {
            jsonObject_sh = JSONUtil.parseObj(result_sh);
        } catch (Exception e) {
            log.error("调取上海有色接口失败", e.getMessage());
        }
        return jsonObject_sh;
    }

    @Nullable
    private JSONObject getLunTong() {
        HashMap<String, Object> paramMap_LD = new HashMap<>();
        paramMap_LD.put("marketVmid", MARKETVMID_LDQH);
        String result_ld = HttpUtil.post(URL_LDQH_API, paramMap_LD, timeout);
        JSONObject jsonObject_ld = null;
        try {
            jsonObject_ld = JSONUtil.parseObj(result_ld);
//            Thread.sleep(5 * 1000); //设置暂停的时间 5 秒
        } catch (Exception e) {
            log.error("调取伦敦期货接口失败", e.getMessage());
        }
        return jsonObject_ld;
    }

    @Nullable
    private JSONObject getChangJiang() {
        HashMap<String, Object> paramMap_CJ = new HashMap<>();
        paramMap_CJ.put("marketVmid", MARKETVMID_CJXH);
        String result_cj = HttpUtil.post(URL_CJXH_API, paramMap_CJ, timeout);
        JSONObject jsonObject_cj = null;
        try {
            jsonObject_cj = JSONUtil.parseObj(result_cj);
//            Thread.sleep(5 * 1000); //设置暂停的时间 5 秒
        } catch (Exception e) {
            log.error("调取长江现货接口失败", e.getMessage());
        }
        return jsonObject_cj;
    }

    @Override
    public void publishTongJiaLunDun() {

    }

    /**
     * 解析长江现货接口返回接口 ，抽取有用数据
     *
     * @param sb_price
     * @param sb_desc
     * @param nowDate
     * @param jsonObject
     * @return
     */
    private Map<String, Object> getChangJiangStringObjectMap(StringBuffer sb_price, StringBuffer sb_desc, String nowDate, JSONObject jsonObject) {
        if (null == sb_desc) {
            sb_desc = new StringBuffer();
        }
        JSONObject body = JSONUtil.parseObj(jsonObject.get("body"));
        JSONArray priceList = JSONUtil.parseArray(body.get("priceList"));
        AtomicReference<String> publishDate = new AtomicReference<>("");
        AtomicReference<String> titleZhangDie = new AtomicReference<>("");
        AtomicReference<String> marketName = new AtomicReference<>("");
        AtomicReference<String> tong = new AtomicReference<>("");
        AtomicReference<String> tongZhangDie = new AtomicReference<>("");
        AtomicReference<String> tong_ = new AtomicReference<>("");
        AtomicReference<String> tongPrice = new AtomicReference<>("");
        AtomicReference<String> lv = new AtomicReference<>("");
        AtomicReference<String> lvZhangDie = new AtomicReference<>("");
        AtomicReference<String> lv_ = new AtomicReference<>("");
        AtomicReference<String> wuYangTongSi = new AtomicReference<>("");
        AtomicReference<String> wuYangTongSiZhangDie = new AtomicReference<>("");
        AtomicReference<String> wuYangTongSi_ = new AtomicReference<>("");
        AtomicReference<String> qiBaoXian = new AtomicReference<>("");
        AtomicReference<String> qiBaoXianZhangDie = new AtomicReference<>("");
        AtomicReference<String> qiBaoXian_ = new AtomicReference<>("");
        AtomicReference<String> unit = new AtomicReference<>("");
        StringBuffer cjContent = new StringBuffer();
        StringBuffer cjPrice = new StringBuffer();
        StringBuffer cjPriceTable = new StringBuffer();
        StringBuffer sb_desc_temp = new StringBuffer();
//        cjContent.append(ZhaoLanConst.CJXH_PRE);
        priceList.stream().map(price -> {
            HashMap<String, Object> hashMap = JsonObjectToHashMap((JSONObject) price);
            return hashMap;
        }).filter(price -> {
            //品名
            String name = price.get("productSortName").toString();
            if (("1#铜").equals(name) || "无氧铜丝(硬)".equals(name) || "漆包线".equals(name) || "A00铝".equals(name)) {
                return true;
            }
            return false;
        }).forEach(price -> {
            String name = price.get("productSortName").toString();
            //长江现货
            marketName.set(price.get("marketName").toString());
            //发布时间
            String publishDateApi = price.get("publishDate").toString();

            publishDate.set(publishDateApi);
            //品名
            String productSortName = price.get("productSortName").toString();
            String zhangDieStr;
            //均价
            String avgPrice = price.get("avgPrice").toString();
            if (avgPrice.indexOf(".") > 0) {
                //正则表达
                avgPrice = avgPrice.replaceAll("0+?$", "");//去掉后面无用的零
                avgPrice = avgPrice.replaceAll("[.]$", "");//如小数点后面全是零则去掉小数点
            }
            //涨跌
            String highsLowsAmount = price.get("highsLowsAmount").toString();
            String highsLowsAmountStr;
            if (highsLowsAmount.indexOf(".") > 0) {
                //正则表达
                highsLowsAmount = highsLowsAmount.replaceAll("0+?$", "");//去掉后面无用的零
                highsLowsAmount = highsLowsAmount.replaceAll("[.]$", "");//如小数点后面全是零则去掉小数点
            }
            //单位
            String unitStr = price.get("unit").toString();
            unit.set(unitStr);
            if (highsLowsAmount.indexOf("-") > -1) {
                highsLowsAmount = highsLowsAmount.substring(1, highsLowsAmount.length());
                highsLowsAmountStr = "<font color=#00D100>**" + highsLowsAmount + "**</font>";

                zhangDieStr = "跌";
                titleZhangDie.set("下跌");
            } else {
                highsLowsAmountStr = "<font color=#FF4C41>**" + highsLowsAmount + "**</font>";
                zhangDieStr = "涨";
                titleZhangDie.set("上涨");
            }
            cjPriceTable.append("| " + name + " | " + "<font color=#007AAA>**" + avgPrice + "**</font> | " + highsLowsAmountStr + " | " + publishDateApi + " |\n");
            cjPrice.append(name + "：<font color=#007AAA>**" + avgPrice + "**</font>" + unitStr + "，" + zhangDieStr + highsLowsAmountStr + unitStr + "\n");
            //摘要
            sb_desc_temp.append(name + "：" + avgPrice + "" + unitStr + "，" + zhangDieStr + highsLowsAmount + unitStr + "\n");
            switch (name) {
                case "1#铜":
                    tong.set(avgPrice);
                    tongZhangDie.set(zhangDieStr);
                    tong_.set(highsLowsAmountStr);
                    tongPrice.set(highsLowsAmount);
                    break;
                case "无氧铜丝(硬)":
                    wuYangTongSi.set(avgPrice);
                    wuYangTongSiZhangDie.set(zhangDieStr);
                    wuYangTongSi_.set(highsLowsAmountStr);
                    break;
                case "漆包线":
                    qiBaoXian.set(avgPrice);
                    qiBaoXianZhangDie.set(zhangDieStr);
                    qiBaoXian_.set(highsLowsAmountStr);
                    break;
                case "A00铝":
                    lv.set(avgPrice);
                    lvZhangDie.set(zhangDieStr);
                    lv_.set(highsLowsAmountStr);
                    break;
                default:
                    break;
            }

        });
        String publishDateStr = publishDate.get();
        //长江期货内容
        cjContent.append(marketName.get())
                .append(" &nbsp;")
                .append(publishDateStr)
                //.append(ZhaoLanConst.CJXH_PRE2)
                .append(cjPrice);
        sb_price.append(
                "#### 市场：" + marketName.get() + "\n"
                        + "- 单位：" + unit + "\n" +
                        "\n" +
                        "| 品名 | 均价 | 涨跌 | 时间 |\n" +
                        "| --- | --- | --- |--- |\n")
                .append(cjPriceTable)
                .append("\n")
                .append(cjPrice);
        sb_desc.append(marketName.get()).append("  ").append(publishDateStr + "\n")
                .append(sb_desc_temp);
        Map<String, Object> params = new HashMap<>();
        params.put("marketName", marketName.get());
        params.put("publishDate", publishDateStr);
        params.put("unit", unit.get());
        params.put("tong", tong.get());
        params.put("tongZhangDie", tongZhangDie.get());
        params.put("tong_", tong_.get());
        params.put("tongPrice", tongPrice.get());
        params.put("wuYangTongSi", wuYangTongSi.get());
        params.put("wuYangTongSi_", wuYangTongSi_.get());
        params.put("wuYangTongSiZhangDie", wuYangTongSiZhangDie.get());
        params.put("qiBaoXian", qiBaoXian.get());
        params.put("qiBaoXian_", qiBaoXian_.get());
        params.put("qiBaoXianZhangDie", qiBaoXianZhangDie.get());
        params.put("lv", lv.get());
        params.put("lv_", lv_.get());
        params.put("lvZhangDie", lvZhangDie.get());
        params.put("titleZhangDie", titleZhangDie.get());
        params.put("nowDate", nowDate);
        params.put("cjContent", cjContent.toString());

        return params;
    }

    /**
     * 解析伦敦期货接口返回数据
     *
     * @param sb_price
     * @param sb_desc
     * @param nowDate
     * @param jsonObject
     * @return
     */
    private Map<String, Object> getLunDunStringObjectMap(StringBuffer sb_price, StringBuffer sb_desc, String nowDate, JSONObject jsonObject) {
        if (null == sb_desc) {
            sb_desc = new StringBuffer();
        }
        JSONObject body = JSONUtil.parseObj(jsonObject.get("body"));
        JSONArray priceList = JSONUtil.parseArray(body.get("priceList"));
        AtomicReference<String> publishDate = new AtomicReference<>("");
        AtomicReference<String> titleZhangDie = new AtomicReference<>("");
        AtomicReference<String> marketName = new AtomicReference<>("");
        AtomicReference<String> unit = new AtomicReference<>("");
        StringBuffer ldPrice = new StringBuffer();
        StringBuffer ldPriceTable = new StringBuffer();
        StringBuffer sb_desc_temp = new StringBuffer();
        priceList.stream().map(price -> {
            HashMap<String, Object> hashMap = JsonObjectToHashMap((JSONObject) price);
            return hashMap;
        }).forEach(price -> {
            String name = price.get("productSortName").toString();
            //长江现货
            marketName.set(price.get("marketName").toString());
            //发布时间
            String publishDateApi = price.get("publishDate").toString();

            publishDate.set(publishDateApi);
            //品名
            String productSortName = price.get("productSortName").toString();
            String zhangDieStr;
            //最新价
            String avgPrice = price.get("newsPrice").toString();
            if (avgPrice.indexOf(".") > 0) {
                //正则表达
                avgPrice = avgPrice.replaceAll("0+?$", "");//去掉后面无用的零
                avgPrice = avgPrice.replaceAll("[.]$", "");//如小数点后面全是零则去掉小数点
            }
            //涨跌
            String highsLowsAmount = price.get("highsLowsAmount").toString();
            String highsLowsAmountStr;
            if (highsLowsAmount.indexOf(".") > 0) {
                //正则表达
                highsLowsAmount = highsLowsAmount.replaceAll("0+?$", "");//去掉后面无用的零
                highsLowsAmount = highsLowsAmount.replaceAll("[.]$", "");//如小数点后面全是零则去掉小数点
            }
            //单位
            String unitStr = price.get("unit").toString();
            unit.set(unitStr);
            if (highsLowsAmount.indexOf("-") > -1) {
                highsLowsAmount = highsLowsAmount.substring(1, highsLowsAmount.length());
                highsLowsAmountStr = "<font color=#00D100>**" + highsLowsAmount + "**</font>";

                zhangDieStr = "跌";
                titleZhangDie.set("下跌");
            } else {
                highsLowsAmountStr = "<font color=#FF4C41>**" + highsLowsAmount + "**</font>";
                zhangDieStr = "涨";
                titleZhangDie.set("上涨");
            }
            ldPriceTable.append("| " + name + " | " + "<font color=#007AAA>**" + avgPrice + "**</font> | " + highsLowsAmountStr + " | " + publishDateApi + " |\n");
            ldPrice.append(name + "：<font color=#007AAA>**" + avgPrice + "**</font>" + unitStr + "，" + zhangDieStr + highsLowsAmountStr + unitStr + "\n");
            //摘要
            sb_desc_temp.append(name + "：" + avgPrice + "" + unitStr + "，" + zhangDieStr + highsLowsAmount + unitStr + "\n");


        });
        String publishDateStr = publishDate.get();
        sb_price.append(
                "#### 市场：" + marketName.get() + "\n"
                        + "- 单位：" + unit + "\n" +
                        "\n" +
                        "| 品名 | 最新价 | 涨跌 | 时间 |\n" +
                        "| --- | --- | --- |--- |\n")
                .append(ldPriceTable)
                .append("\n")
                .append(ldPrice);
        sb_desc.append(marketName.get()).append("  ").append(publishDateStr + "\n")
                .append(sb_desc_temp);
        Map<String, Object> params = new HashMap<>();
        params.put("marketName", marketName.get());
        params.put("publishDate", publishDateStr);
        params.put("unit", unit.get());
        params.put("titleZhangDie", titleZhangDie.get());

        return params;
    }

    /**
     * 上海现货
     *
     * @param sb_price
     * @param sb_desc
     * @param nowDate
     * @param jsonObject
     * @return
     */
    private Map<String, Object> getShangHaiStringObjectMap(StringBuffer sb_price, StringBuffer sb_desc, String nowDate, JSONObject jsonObject) {
        if (null == sb_desc) {
            sb_desc = new StringBuffer();
        }
        JSONObject body = JSONUtil.parseObj(jsonObject.get("body"));
        JSONArray priceList = JSONUtil.parseArray(body.get("priceList"));
        AtomicReference<String> publishDate = new AtomicReference<>("");
        AtomicReference<String> titleZhangDie = new AtomicReference<>("");
        AtomicReference<String> marketName = new AtomicReference<>("");
        AtomicReference<String> unit = new AtomicReference<>("");
        StringBuffer ldPrice = new StringBuffer();
        StringBuffer ldPriceTable = new StringBuffer();
        StringBuffer sb_desc_temp = new StringBuffer();
        priceList.stream().map(price -> {
            HashMap<String, Object> hashMap = JsonObjectToHashMap((JSONObject) price);
            return hashMap;
        }).forEach(price -> {
            String name = price.get("productSortName").toString();
            //物贸现货 上海有色
//            marketName.set(price.get("marketName").toString());
            marketName.set("上海有色");
            //发布时间
            String publishDateApi = price.get("publishDate").toString();

            publishDate.set(publishDateApi);
            //品名
            String productSortName = price.get("productSortName").toString();
            String zhangDieStr;
            //均价
            String avgPrice = price.get("avgPrice").toString();
            if (avgPrice.indexOf(".") > 0) {
                //正则表达
                avgPrice = avgPrice.replaceAll("0+?$", "");//去掉后面无用的零
                avgPrice = avgPrice.replaceAll("[.]$", "");//如小数点后面全是零则去掉小数点
            }
            //涨跌
            String highsLowsAmount = price.get("highsLowsAmount").toString();
            String highsLowsAmountStr;
            if (highsLowsAmount.indexOf(".") > 0) {
                //正则表达
                highsLowsAmount = highsLowsAmount.replaceAll("0+?$", "");//去掉后面无用的零
                highsLowsAmount = highsLowsAmount.replaceAll("[.]$", "");//如小数点后面全是零则去掉小数点
            }
            //单位
            String unitStr = price.get("unit").toString();
            unit.set(unitStr);
            if (highsLowsAmount.indexOf("-") > -1) {
                highsLowsAmount = highsLowsAmount.substring(1, highsLowsAmount.length());
                highsLowsAmountStr = "<font color=#00D100>**" + highsLowsAmount + "**</font>";

                zhangDieStr = "跌";
                titleZhangDie.set("下跌");
            } else {
                highsLowsAmountStr = "<font color=#FF4C41>**" + highsLowsAmount + "**</font>";
                zhangDieStr = "涨";
                titleZhangDie.set("上涨");
            }
            ldPriceTable.append("| " + name + " | " + "<font color=#007AAA>**" + avgPrice + "**</font> | " + highsLowsAmountStr + " | " + publishDateApi + " |\n");
            ldPrice.append(name + "：<font color=#007AAA>**" + avgPrice + "**</font>" + unitStr + "，" + zhangDieStr + highsLowsAmountStr + unitStr + "\n");
            //摘要
            sb_desc_temp.append(name + "：" + avgPrice + "" + unitStr + "，" + zhangDieStr + highsLowsAmount + unitStr + "\n");


        });
        String publishDateStr = publishDate.get();
        sb_price.append(
                "#### 市场：" + marketName.get() + "\n"
                        + "- 单位：" + unit + "\n" +
                        "\n" +
                        "| 品名 | 均价 | 涨跌 | 时间 |\n" +
                        "| --- | --- | --- |--- |\n")
                .append(ldPriceTable)
                .append("\n")
                .append(ldPrice);
        sb_desc.append(marketName.get()).append("  ").append(publishDateStr + "\n")
                .append(sb_desc_temp);
        Map<String, Object> params = new HashMap<>();
        params.put("marketName", marketName.get());
        params.put("publishDate", publishDateStr);
        params.put("unit", unit.get());
        params.put("titleZhangDie", titleZhangDie.get());

        return params;
    }

    /**
     * 日期转时间
     *
     * @param date
     * @return
     */
    public static String date2string(Date date) {
        return date2string(date, YYYY_MM_DD);
    }

    public static String date2string(Date date, String format) {
        String strDate;
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        strDate = sdf.format(date);
        return strDate;
    }

    //1.將JSONObject對象轉換為HashMap<String,String>
    public static HashMap<String, Object> JsonObjectToHashMap(JSONObject jsonObj) {
        HashMap<String, Object> data = new HashMap<>();
        Iterator it = jsonObj.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = (Map.Entry<String, Object>) it.next();
            data.put(entry.getKey(), entry.getValue());
        }
        return data;
    }

}
