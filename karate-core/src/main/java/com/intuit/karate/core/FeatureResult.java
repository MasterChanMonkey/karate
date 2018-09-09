/*
 * The MIT License
 *
 * Copyright 2018 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.core;

import com.intuit.karate.FileUtils;
import com.intuit.karate.JsonUtils;
import com.intuit.karate.ScriptValueMap;
import com.intuit.karate.StringUtils;
import com.intuit.karate.exception.KarateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author pthomas3
 */
public class FeatureResult {

    private final Feature feature;
    private final String displayName;
    private final List<ScenarioResult> scenarioResults = new ArrayList();

    private int scenarioCount;
    private int failedCount;
    private List<Throwable> errors;
    private double duration;

    private ScriptValueMap resultVars;
    private Map<String, Object> callArg;
    private int loopIndex;

    public void printStats(String featurePath, String reportPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("---------------------------------------------------------\n");
        sb.append("feature: ").append(featurePath).append('\n');
        sb.append("report: ").append(reportPath).append('\n');
        sb.append(String.format("scenarios: %2d | passed: %2d | failed: %2d | time: %.2f\n", scenarioCount, scenarioCount - failedCount, failedCount, duration));
        sb.append("---------------------------------------------------------\n");
        System.out.println(sb);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap(8);
        List<Map> list = new ArrayList(scenarioResults.size());
        map.put("elements", list);
        for (ScenarioResult re : scenarioResults) {
            if (re.getScenario().getFeature().isBackgroundPresent()) {
                list.add(re.backgroundToMap());
            }
            list.add(re.toMap());
        }
        map.put("keyword", Feature.KEYWORD);
        map.put("line", feature.getLine());
        map.put("uri", displayName);
        map.put("name", displayName);
        map.put("id", StringUtils.toIdString(feature.getName()));
        String temp = feature.getName() == null ? "" : feature.getName();
        if (feature.getDescription() != null) {
            temp = temp + "\n" + feature.getDescription();
        }
        map.put("description", temp.trim());
        if (feature.getTags() != null) {
            map.put("tags", Tags.toResultList(feature.getTags()));
        }
        return map;
    }

    // this "flattens" all steps from all scenarios
    public List<StepResult> getStepResults() {
        List<StepResult> list = new ArrayList();
        for (ScenarioResult sr : scenarioResults) {
            list.addAll(sr.getStepResults());
        }
        return list;
    }

    public FeatureResult(Feature feature) {
        this.feature = feature;
        displayName = FileUtils.removePrefix(feature.getRelativePath());
    }

    public Feature getFeature() {
        return feature;
    }

    public String getPackageQualifiedName() {
        return feature.getResource().getPackageQualifiedName();
    }

    public String getDisplayUri() {
        return displayName;
    }

    public KarateException getErrorsCombined() {
        if (errors == null) {
            return null;
        }
        if (errors.size() == 1) {
            Throwable error = errors.get(0);
            if (error instanceof KarateException) {
                return (KarateException) error;
            } else {
                return new KarateException("call failed", error);
            }
        }
        return new KarateException(getErrorMessages());
    }

    public String getErrorMessages() {
        StringBuilder sb = new StringBuilder();
        Iterator<Throwable> iterator = errors.iterator();
        while (iterator.hasNext()) {
            Throwable error = iterator.next();
            sb.append(error.getMessage());
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    public String getCallName() {
        String append = loopIndex == -1 ? "" : "[" + loopIndex + "] ";
        return append + displayName;
    }

    public String getCallArgPretty() {
        if (callArg == null) {
            return null;
        }
        return JsonUtils.toPrettyJsonString(JsonUtils.toJsonDoc(callArg));
    }

    public Map<String, Object> getCallArg() {
        return callArg;
    }

    public void setCallArg(Map<String, Object> callArg) {
        this.callArg = callArg;
    }

    public int getLoopIndex() {
        return loopIndex;
    }

    public void setLoopIndex(int loopIndex) {
        this.loopIndex = loopIndex;
    }

    public double getDuration() {
        return duration;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getScenarioCount() {
        return scenarioCount;
    }

    public boolean isFailed() {
        return errors != null && !errors.isEmpty();
    }

    public List<Throwable> getErrors() {
        return errors;
    }

    public Map<String, Object> getResultAsPrimitiveMap() {
        if (resultVars == null) {
            return Collections.EMPTY_MAP;
        }
        return resultVars.toPrimitiveMap();
    }

    public void setResultVars(ScriptValueMap resultVars) {
        this.resultVars = resultVars;
    }

    private void addError(Throwable error) {
        failedCount++;
        if (errors == null) {
            errors = new ArrayList();
        }
        errors.add(error);
    }

    public void addResult(ScenarioResult result) {
        scenarioResults.add(result);
        duration += Engine.nanosToSeconds(result.getDuration());
        scenarioCount++;
        if (result.isFailed()) {
            addError(result.getError());
        }
    }

    public List<ScenarioResult> getScenarioResults() {
        return scenarioResults;
    }

}