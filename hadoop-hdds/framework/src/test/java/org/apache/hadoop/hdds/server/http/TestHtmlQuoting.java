/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hdds.server.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

/**
 * Testing HTML Quoting.
 */
public class TestHtmlQuoting {

  @Test
  public void testNeedsQuoting() throws Exception {
    assertTrue(HtmlQuoting.needsQuoting("abcde>"));
    assertTrue(HtmlQuoting.needsQuoting("<abcde"));
    assertTrue(HtmlQuoting.needsQuoting("abc'de"));
    assertTrue(HtmlQuoting.needsQuoting("abcde\""));
    assertTrue(HtmlQuoting.needsQuoting("&"));
    assertFalse(HtmlQuoting.needsQuoting(""));
    assertFalse(HtmlQuoting.needsQuoting("ab\ncdef"));
    assertFalse(HtmlQuoting.needsQuoting(null));
  }

  @Test
  public void testQuoting() throws Exception {
    assertEquals("ab&lt;cd", HtmlQuoting.quoteHtmlChars("ab<cd"));
    assertEquals("ab&gt;", HtmlQuoting.quoteHtmlChars("ab>"));
    assertEquals("&amp;&amp;&amp;", HtmlQuoting.quoteHtmlChars("&&&"));
    assertEquals(" &apos;\n", HtmlQuoting.quoteHtmlChars(" '\n"));
    assertEquals("&quot;", HtmlQuoting.quoteHtmlChars("\""));
    assertNull(HtmlQuoting.quoteHtmlChars(null));
  }

  private void runRoundTrip(String str) throws Exception {
    assertEquals(str,
        HtmlQuoting.unquoteHtmlChars(HtmlQuoting.quoteHtmlChars(str)));
  }

  @Test
  public void testRoundtrip() throws Exception {
    runRoundTrip("");
    runRoundTrip("<>&'\"");
    runRoundTrip("ab>cd<ef&ghi'\"");
    runRoundTrip("A string\n with no quotable chars in it!");
    runRoundTrip(null);
    StringBuilder buffer = new StringBuilder();
    for (char ch = 0; ch < 127; ++ch) {
      buffer.append(ch);
    }
    runRoundTrip(buffer.toString());
  }

  @Test
  public void testRequestQuoting() throws Exception {
    HttpServletRequest mockReq = mock(HttpServletRequest.class);
    HttpServer2.QuotingInputFilter.RequestQuoter quoter =
        new HttpServer2.QuotingInputFilter.RequestQuoter(mockReq);

    doReturn("a<b").when(mockReq).getParameter("x");
    assertEquals("a&lt;b", quoter.getParameter("x"),
        "Test simple param quoting");

    doReturn(null).when(mockReq).getParameter("x");
    assertNull(quoter.getParameter("x"),
        "Test that missing parameters don't cause NPE");

    doReturn(new String[] {"a<b", "b"}).when(mockReq)
        .getParameterValues("x");
    assertArrayEquals(new String[] {"a&lt;b", "b"},
        quoter.getParameterValues("x"), "Test escaping of an array");

    doReturn(null).when(mockReq).getParameterValues("x");
    assertNull(quoter.getParameterValues("x"),
        "Test that missing parameters don't cause NPE for array");
  }
}
