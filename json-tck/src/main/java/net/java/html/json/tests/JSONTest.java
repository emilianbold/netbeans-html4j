/**
 * HTML via Java(tm) Language Bindings
 * Copyright (C) 2013 Jaroslav Tulach <jaroslav.tulach@apidesign.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details. apidesign.org
 * designates this particular file as subject to the
 * "Classpath" exception as provided by apidesign.org
 * in the License file that accompanied this code.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://wiki.apidesign.org/wiki/GPLwithClassPathException
 */
package net.java.html.json.tests;

import java.io.ByteArrayInputStream;
import net.java.html.BrwsrCtx;
import net.java.html.json.Model;
import net.java.html.json.Models;
import net.java.html.json.OnReceive;
import net.java.html.json.Property;
import org.apidesign.bck2brwsr.vmtest.BrwsrTest;
import org.apidesign.bck2brwsr.vmtest.Http;
import org.apidesign.bck2brwsr.vmtest.VMTest;
import org.apidesign.html.json.impl.JSON;

/** Need to verify that models produce reasonable JSON objects.
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
@Model(className = "JSONik", properties = {
    @Property(name = "fetched", type = Person.class),
    @Property(name = "fetchedCount", type = int.class),
    @Property(name = "fetchedResponse", type = String.class),
    @Property(name = "fetchedSex", type = Sex.class, array = true)
})
public final class JSONTest {
    private JSONik js;
    private Integer orig;
    
    @BrwsrTest public void toJSONInABrowser() throws Throwable {
        Person p = Models.bind(new Person(), Utils.newContext());
        p.setSex(Sex.MALE);
        p.setFirstName("Jarda");
        p.setLastName("Tulach");

        Object json;
        try {
            json = parseJSON(p.toString());
        } catch (Throwable ex) {
            throw new IllegalStateException("Can't parse " + p).initCause(ex);
        }
        
        Person p2 = JSON.read(Utils.newContext(), Person.class, json);
        
        assert p2.getFirstName().equals(p.getFirstName()) : 
            "Should be the same: " + p.getFirstName() + " != " + p2.getFirstName();
    }
    
    @OnReceive(url="/{url}")
    static void fetch(Person p, JSONik model) {
        model.setFetched(p);
    }

    @OnReceive(url="/{url}")
    static void fetchArray(Person[] p, JSONik model) {
        model.setFetchedCount(p.length);
        model.setFetched(p[0]);
    }
    
    @OnReceive(url="/{url}")
    static void fetchPeople(People p, JSONik model) {
        model.setFetchedCount(p.getInfo().size());
        model.setFetched(p.getInfo().get(0));
    }

    @OnReceive(url="/{url}")
    static void fetchPeopleAge(People p, JSONik model) {
        int sum = 0;
        for (int a : p.getAge()) {
            sum += a;
        }
        model.setFetchedCount(sum);
    }
    
    @Http(@Http.Resource(
        content = "{'firstName': 'Sitar', 'sex': 'MALE'}", 
        path="/person.json", 
        mimeType = "application/json"
    ))
    @BrwsrTest public void loadAndParseJSON() throws InterruptedException {
        if (js == null) {
            js = Models.bind(new JSONik(), Utils.newContext());
            js.applyBindings();

            js.fetch("person.json");
        }
    
        Person p = js.getFetched();
        if (p == null) {
            throw new InterruptedException();
        }
        
        assert "Sitar".equals(p.getFirstName()) : "Expecting Sitar: " + p.getFirstName();
        assert Sex.MALE.equals(p.getSex()) : "Expecting MALE: " + p.getSex();
    }
    
    @OnReceive(url="/{url}?callme={me}", jsonp = "me")
    static void fetchViaJSONP(Person p, JSONik model) {
        model.setFetched(p);
    }
    
    @Http(@Http.Resource(
        content = "$0({'firstName': 'Mitar', 'sex': 'MALE'})", 
        path="/person.json", 
        mimeType = "application/javascript",
        parameters = { "callme" }
    ))
    @BrwsrTest public void loadAndParseJSONP() throws InterruptedException, Exception {
        
        if (js == null) {
            orig = scriptElements();
            assert orig > 0 : "There should be some scripts on the page";
            
            js = Models.bind(new JSONik(), Utils.newContext());
            js.applyBindings();

            js.fetchViaJSONP("person.json");
        }
    
        Person p = js.getFetched();
        if (p == null) {
            throw new InterruptedException();
        }
        
        assert "Mitar".equals(p.getFirstName()) : "Unexpected: " + p.getFirstName();
        assert Sex.MALE.equals(p.getSex()) : "Expecting MALE: " + p.getSex();
        
        int now = scriptElements();
        
        assert orig == now : "The set of elements is unchanged. Delta: " + (now - orig);
    }
    
    
    
    @OnReceive(url="{url}", method = "PUT", data = Person.class)
    static void putPerson(JSONik model, String reply) {
        model.setFetchedCount(1);
        model.setFetchedResponse(reply);
    }
    @Http(@Http.Resource(
        content = "$0\n$1", 
        path="/person.json", 
        mimeType = "text/plain",
        parameters = { "http.method", "http.requestBody" }
    ))
    @BrwsrTest public void putPeopleUsesRightMethod() throws InterruptedException, Exception {
        if (js == null) {
            orig = scriptElements();
            assert orig > 0 : "There should be some scripts on the page";
            
            js = Models.bind(new JSONik(), Utils.newContext());
            js.applyBindings();

            Person p = Models.bind(new Person(), BrwsrCtx.EMPTY);
            p.setFirstName("Jarda");
            js.putPerson("person.json", p);
        }
    
        int cnt = js.getFetchedCount();
        if (cnt == 0) {
            throw new InterruptedException();
        }
        String res = js.getFetchedResponse();
        int line = res.indexOf('\n');
        String msg;
        if (line >= 0) {
            msg = res.substring(line + 1);
            res = res.substring(0, line);
        } else {
            msg = res;
        }
        
        assert "PUT".equals(res) : "Server was queried with PUT method: " + js.getFetchedResponse();
        
        assert msg.contains("Jarda") : "Data transferred to the server: " + msg;
    }
    
    private static int scriptElements() throws Exception {
        return ((Number)Utils.executeScript("return window.document.getElementsByTagName('script').length;")).intValue();
    }

    private static Object parseJSON(String s) throws Exception {
        return Utils.executeScript("return window.JSON.parse(arguments[0]);", s);
    }
    
    @Http(@Http.Resource(
        content = "{'firstName': 'Sitar', 'sex': 'MALE'}", 
        path="/person.json", 
        mimeType = "application/json"
    ))
    @BrwsrTest public void loadAndParseJSONSentToArray() throws InterruptedException {
        if (js == null) {
            js = Models.bind(new JSONik(), Utils.newContext());
            js.applyBindings();

            js.fetchArray("person.json");
        }
        
        Person p = js.getFetched();
        if (p == null) {
            throw new InterruptedException();
        }
        
        assert "Sitar".equals(p.getFirstName()) : "Expecting Sitar: " + p.getFirstName();
        assert Sex.MALE.equals(p.getSex()) : "Expecting MALE: " + p.getSex();
    }
    
    @Http(@Http.Resource(
        content = "[{'firstName': 'Gitar', 'sex': 'FEMALE'}]", 
        path="/person.json", 
        mimeType = "application/json"
    ))
    @BrwsrTest public void loadAndParseJSONArraySingle() throws InterruptedException {
        if (js == null) {
            js = Models.bind(new JSONik(), Utils.newContext());
            js.applyBindings();
        
            js.fetch("person.json");
        }
        
        Person p = js.getFetched();
        if (p == null) {
            throw new InterruptedException();
        }
        
        assert "Gitar".equals(p.getFirstName()) : "Expecting Gitar: " + p.getFirstName();
        assert Sex.FEMALE.equals(p.getSex()) : "Expecting FEMALE: " + p.getSex();
    }
    
    @Http(@Http.Resource(
        content = "{'info':[{'firstName': 'Gitar', 'sex': 'FEMALE'}]}", 
        path="/people.json", 
        mimeType = "application/json"
    ))
    @BrwsrTest public void loadAndParseArrayInPeople() throws InterruptedException {
        if (js == null) {
            js = Models.bind(new JSONik(), Utils.newContext());
            js.applyBindings();
        
            js.fetchPeople("people.json");
        }
        
        if (0 == js.getFetchedCount()) {
            throw new InterruptedException();
        }

        assert js.getFetchedCount() == 1 : "One person loaded: " + js.getFetchedCount();
        
        Person p = js.getFetched();
        
        assert p != null : "We should get our person back: " + p;
        assert "Gitar".equals(p.getFirstName()) : "Expecting Gitar: " + p.getFirstName();
        assert Sex.FEMALE.equals(p.getSex()) : "Expecting FEMALE: " + p.getSex();
    }
    
    @Http(@Http.Resource(
        content = "{'age':[1, 2, 3]}", 
        path="/people.json", 
        mimeType = "application/json"
    ))
    @BrwsrTest public void loadAndParseArrayOfIntegers() throws InterruptedException {
        if (js == null) {
            js = Models.bind(new JSONik(), Utils.newContext());
            js.applyBindings();
        
            js.fetchPeopleAge("people.json");
        }
        
        if (0 == js.getFetchedCount()) {
            throw new InterruptedException();
        }

        assert js.getFetchedCount() == 6 : "1 + 2 + 3 is " + js.getFetchedCount();
    }
    
    @OnReceive(url="/{url}")
    static void fetchPeopleSex(People p, JSONik model) {
        model.setFetchedCount(1);
        model.getFetchedSex().addAll(p.getSex());
    }
    
    
    @Http(@Http.Resource(
        content = "{'sex':['FEMALE', 'MALE', 'MALE']}", 
        path="/people.json", 
        mimeType = "application/json"
    ))
    @BrwsrTest public void loadAndParseArrayOfEnums() throws InterruptedException {
        if (js == null) {
            js = Models.bind(new JSONik(), Utils.newContext());
            js.applyBindings();
        
            js.fetchPeopleSex("people.json");
        }
        
        if (0 == js.getFetchedCount()) {
            throw new InterruptedException();
        }

        assert js.getFetchedCount() == 1 : "Loaded";
        
        assert js.getFetchedSex().size() == 3 : "Three values " + js.getFetchedSex();
        assert js.getFetchedSex().get(0) == Sex.FEMALE : "Female first " + js.getFetchedSex();
        assert js.getFetchedSex().get(1) == Sex.MALE : "male 2nd " + js.getFetchedSex();
        assert js.getFetchedSex().get(2) == Sex.MALE : "male 3rd " + js.getFetchedSex();
    }
    
    @Http(@Http.Resource(
        content = "[{'firstName': 'Gitar', 'sex': 'FEMALE'},"
        + "{'firstName': 'Peter', 'sex': 'MALE'}"
        + "]", 
        path="/person.json", 
        mimeType = "application/json"
    ))
    @BrwsrTest public void loadAndParseJSONArray() throws InterruptedException {
        if (js == null) {
            js = Models.bind(new JSONik(), Utils.newContext());
            js.applyBindings();
            js.fetchArray("person.json");
        }
        
        
        Person p = js.getFetched();
        if (p == null) {
            throw new InterruptedException();
        }
        
        assert js.getFetchedCount() == 2 : "We got two values: " + js.getFetchedCount();
        assert "Gitar".equals(p.getFirstName()) : "Expecting Gitar: " + p.getFirstName();
        assert Sex.FEMALE.equals(p.getSex()) : "Expecting FEMALE: " + p.getSex();
    }
    
    @Model(className = "NameAndValue", properties = {
        @Property(name = "name", type = String.class),
        @Property(name = "value", type = long.class),
        @Property(name = "small", type = byte.class)
    })
    static class NandV {
    }
    
    @BrwsrTest public void parseNullNumber() throws Exception {
        String txt = "{ \"name\":\"M\" }";
        ByteArrayInputStream is = new ByteArrayInputStream(txt.getBytes("UTF-8"));
        NameAndValue v = Models.parse(Utils.newContext(), NameAndValue.class, is);
        assert "M".equals(v.getName()) : "Name is 'M': " + v.getName();
        assert 0 == v.getValue() : "Value is empty: " + v.getValue();
        assert 0 == v.getSmall() : "Small value is empty: " + v.getSmall();
    }
    
    static Object[] create() {
        return VMTest.create(JSONTest.class);
    }
    
}