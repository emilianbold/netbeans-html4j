/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2016 Sun Microsystems, Inc.
 */
package net.java.html.boot.truffle;

import java.io.Closeable;
import java.io.IOException;
import net.java.html.lib.Array;
import net.java.html.lib.Function;
import org.netbeans.html.boot.spi.Fn;
import org.testng.Assert;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JsArrayTruffleTest {
    private Fn.Presenter presenter;
    private Closeable close;
    
    @BeforeMethod
    public void initializePresenter() {
        presenter = TrufflePresenters.create(null);
        close = Fn.activate(presenter);
    }

    @AfterMethod
    public void closePresenter() throws IOException {
        close.close();
    }


    @Test
    public void forEachArray() {
        Array<String> array = new Array<>();
        array.push("Hello");
        array.push("World");
        array.push("how");
        array.push("are", "You?");

        Assert.assertEquals(array.length(), 5, "Five words");

        Array<String> lowerCaseArray = new Array<>();
        array.forEach((Function.A1<String, Void>) (String p1) -> {
            lowerCaseArray.push(p1.toLowerCase());
            return null;
        });

        assertEquals(lowerCaseArray.$get(0), "hello");
        assertEquals(lowerCaseArray.$get(1), "world");
        assertEquals(lowerCaseArray.$get(2), "how");
        assertEquals(lowerCaseArray.$get(3), "are");
        assertEquals(lowerCaseArray.$get(4), "you?");
    }

}
