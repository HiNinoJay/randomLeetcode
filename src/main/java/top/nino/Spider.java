package top.nino;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : nino
 * @date : 2024/8/21 18:39
 */
public class Spider {
    private static final String PRFIX_LEETCODE_PROBLEM = "https://leetcode.cn/problems/";
    private static final String HOT_TOP_100_URL = "https://leetcode.cn/studyplan/top-100-liked/";
    private static final String HOT_100 = "hot-100";
    private static final String JSON_SUFFIX = ".json";
    private static final String START_SCRIPT = "<script id=\"__NEXT_DATA__\" type=\"application/json\">";
    private static final String END_SCRIPT = "</script>";
    private static final String WINDOWS_PATH = "\\src\\main\\resources\\";
    private static final String MAX_PATH = "/src/main/resources/";
    private OkHttpClient client;

    private Map<String, String> problemCache;

    public Spider() {
        this.client = new OkHttpClient();
        this.problemCache = new HashMap<>();
    }

    public String fetchHtml(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return response.body().string();
        }
    }

    public void parseHtmlAndLoadCache(String html) throws Exception{

        StringBuilder jsonResult = new StringBuilder(html);
        jsonResult = new StringBuilder(jsonResult.substring(jsonResult.indexOf(START_SCRIPT) + START_SCRIPT.length(), jsonResult.length()));
        jsonResult = new StringBuilder(jsonResult.substring(0, jsonResult.indexOf(END_SCRIPT)));
        JSONObject jsonObject;
        JSONObject preJsonObject;

        try {
            jsonObject = JSONObject.parseObject(jsonResult.toString());


            preJsonObject = loadLocalJson();
            if(preJsonObject == null || !preJsonObject.equals(jsonObject)) {
                writeLocalJson(jsonObject.toString(), getTitle(jsonObject));
            }

        } catch (Exception e) {
            // 去读本地的
            jsonObject = loadLocalJson();
        }

        if(jsonObject == null) {
            throw new RuntimeException("该网址获取题库失败。");
        }

        JSONObject props = (JSONObject)jsonObject.get("props");
        JSONObject pageProps = (JSONObject)props.get("pageProps");
        JSONObject dehydratedState = (JSONObject)pageProps.get("dehydratedState");
        JSONArray queries = dehydratedState.getJSONArray("queries");
        JSONObject firstQuery = (JSONObject) queries.get(0);
        JSONObject state = (JSONObject)firstQuery.get("state");
        JSONObject data = (JSONObject)state.get("data");
        JSONObject studyPlanV2Detail = (JSONObject)data.get("studyPlanV2Detail");
        JSONArray planSubGroups = studyPlanV2Detail.getJSONArray("planSubGroups");

        for(Object object : planSubGroups) {
            JSONObject problemJsonObject = (JSONObject) object;
            JSONArray questionsArray = problemJsonObject.getJSONArray("questions");
            for(Object questionObject : questionsArray) {
                JSONObject questionJsonObject = (JSONObject) questionObject;
                problemCache.put(questionJsonObject.getString("translatedTitle"), PRFIX_LEETCODE_PROBLEM + questionJsonObject.getString("titleSlug"));
            }

        }

    }

    private String getSystemPath() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return WINDOWS_PATH;
        } else if (osName.contains("mac")) {
            return MAX_PATH;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            System.out.println("This is a Unix or Linux system.");
        } else {
            System.out.println("Unknown operating system.");
        }
        return "";
    }

    private void writeLocalJson(String string, String title) {
        getSystemPath();
        try {
            String prePath = Spider.class.getClassLoader().getResource("").getPath();
            String path = prePath.substring(0, prePath.indexOf("/target")) + getSystemPath();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String time = LocalDateTime.now().format(dateTimeFormatter);
            File file = new File(path + title + JSON_SUFFIX);

            if(!file.exists()) {
                // 创建文件
                file.createNewFile();
            } else {
                file = new File(path + title + "-" + time + JSON_SUFFIX);
                file.createNewFile();
            }
            // 写入文件
            Writer write = new OutputStreamWriter(new FileOutputStream(file));
            write.write(string);
            write.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private JSONObject loadLocalJson() {
        try {
            InputStream inputStream = Spider.class.getClassLoader().getResourceAsStream(HOT_100 + JSON_SUFFIX);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String jsonString = bufferedReader.lines().collect(Collectors.joining(""));
            return JSONObject.parseObject(jsonString);
        } catch (Exception e) {
            return null;
        }
    }

    private String getTitle(JSONObject jsonObject) {
        JSONObject props = (JSONObject)jsonObject.get("props");
        JSONObject pageProps = (JSONObject)props.get("pageProps");
        JSONObject dehydratedState = (JSONObject)pageProps.get("dehydratedState");
        JSONArray queries = dehydratedState.getJSONArray("queries");
        JSONObject firstQuery = (JSONObject) queries.get(0);
        JSONObject state = (JSONObject)firstQuery.get("state");
        JSONObject data = (JSONObject)state.get("data");
        JSONObject studyPlanV2Detail = (JSONObject)data.get("studyPlanV2Detail");
        return studyPlanV2Detail.getString("slug");
    }



    public static void main(String[] args) {
        System.out.println("请输入leetcode题库的网址：");
        System.out.println("输入go, 将默认使用 hot 100题库：" + HOT_TOP_100_URL);

        Scanner scanner = new Scanner(System.in);
        String url = scanner.next();

        if("go".equals(url)) {
            url = HOT_TOP_100_URL;
        } else {
            while(url.indexOf("https") != 0) {
                System.out.println("请输入合理的leetcode题库网址。");
                System.out.println("比如hot100题目网址：" + HOT_TOP_100_URL);
                url = scanner.next();
            }
        }



        Spider spider = new Spider();
        try {
            String html = spider.fetchHtml(url);
            spider.parseHtmlAndLoadCache(html);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        int numberSize = 10;
        System.out.println("已载入该题库中的免费题量为：" + spider.problemCache.size());
        System.out.println("请输入你想要的一批的量(输入go采用默认10道题一批)：");
        while(true) {
            String numberString = scanner.next();
            if("go".equals(numberString)) {
                break;
            }
            try {
                numberSize = Integer.parseInt(numberString);
            } catch (Exception e) {
                System.out.println("请输入数字。");
                continue;
            }
            if ( numberSize <= 0 || numberSize > 10) {
                System.out.println("请输入合理的范围。");
                continue;
            }
            break;
        }
        int totalCount = spider.problemCache.size() % numberSize == 0 ? spider.problemCache.size() / numberSize : spider.problemCache.size() / numberSize + 1;
        int count = 0;
        System.out.println("已采用一批的量为：" + numberSize + "(" + count + "/" + totalCount + ")");


        Random random = new Random();
        while(!spider.problemCache.isEmpty()) {
            Scanner input = new Scanner(System.in);
            System.out.println();
            System.out.println("输入next以获取一批。");
            String text = input.next();
            if(!"next".equals(text)) continue;
            System.out.println("(" + ++count + "/" + totalCount + ")");
            List<String> keyList = spider.problemCache.keySet().stream().collect(Collectors.toList());
            for(int i = 1; i <= numberSize; i++) {
                if(spider.problemCache.isEmpty()) {
                    break;
                }
                int index = random.nextInt(keyList.size());
                String key = keyList.get(index);
                if(!spider.problemCache.containsKey(key)) {
                    i--;
                    continue;
                }
                System.out.println(key + ": " + spider.problemCache.get(key));
                spider.problemCache.remove(key);
            }
        }
        System.out.println();
        System.out.println("恭喜，已经通刷一遍。");
    }
}
