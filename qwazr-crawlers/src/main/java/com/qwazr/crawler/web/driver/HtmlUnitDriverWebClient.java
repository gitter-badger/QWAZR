/**
 * Copyright 2014-2015 OpenSearchServer Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.crawler.web.driver;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;

public class HtmlUnitDriverWebClient extends HtmlUnitDriver {

	public HtmlUnitDriverWebClient() {
		super(false);
	}

	public HtmlUnitDriverWebClient(Capabilities capabilities) {
		super(capabilities);
		if (capabilities != null) {
			String language = (String) capabilities
					.getCapability(AdditionalCapabilities.QWAZR_BROWER_LANGUAGE);
			if (language != null) {
				BrowserVersion version = getWebClient().getBrowserVersion();
				version.setBrowserLanguage(language);
				version.setSystemLanguage(language);
				version.setUserLanguage(language);
			}
		}
	}

	@Override
	protected WebClient newWebClient(BrowserVersion version) {
		return super.newWebClient(version.clone());
	}

	@Override
	public WebClient getWebClient() {
		return super.getWebClient();
	}

}