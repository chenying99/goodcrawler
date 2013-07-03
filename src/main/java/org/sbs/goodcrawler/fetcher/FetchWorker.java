/**
 * ##########################  GoodCrawler  ############################
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sbs.goodcrawler.fetcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.sbs.crawler.Worker;
import org.sbs.goodcrawler.conf.jobconf.JobConfiguration;
import org.sbs.goodcrawler.exception.QueueException;
import org.sbs.goodcrawler.job.Page;
import org.sbs.goodcrawler.job.Parser;
import org.sbs.goodcrawler.processor.PendingPages;
import org.sbs.goodcrawler.urlmanager.PendingUrls;
import org.sbs.goodcrawler.urlmanager.WebURL;

/**
 * @author shenbaise(shenbaise@outlook.com)
 * @date 2013-7-1
 * 页面抓取工
 */
public abstract class FetchWorker extends Worker {
	private Log log = LogFactory.getLog(this.getClass());
	protected PendingUrls pendingUrls = PendingUrls.getInstance();
	protected PendingPages pendingPages = PendingPages.getInstace();
	protected PageFetcher fetcher;
	protected JobConfiguration conf;
	protected Parser parser;
	
	public FetchWorker(JobConfiguration conf){
		this.conf = conf;
		parser = new Parser(conf);
	}
	
	public FetchWorker setFetcher(final PageFetcher fetcher){
		this.fetcher = fetcher;
		return this;
	}
	/**
	 * @desc 工作成功
	 */
	public abstract void onSuccessed();
	/**
	 * @desc 工作失败
	 */
	public abstract void onFailed(WebURL url);
	/**
	 * @desc 忽略工作
	 */
	public abstract void onIgnored(WebURL url);
	
	public void fetchPage(WebURL url){
		PageFetchResult result = null;
		if(null!=null && StringUtils.isNotBlank(url.getURL())){
			result = fetcher.fetchHeader(url);
			// 获取状态
			int statusCode = result.getStatusCode();
			if (statusCode == CustomFetchStatus.PageTooBig) {
				onIgnored(url);
				return ;
			}
			if (statusCode != HttpStatus.SC_OK){
				onFailed(url);
			}else {
				Page page = new Page(url);
				pendingUrls.processedSuccess();
				if (!result.fetchContent(page)) {
//					pendingUrls.processedFailure();
					onFailed(url);
				}
				if (!parser.parse(page, url.getURL())) {
//					pendingUrls.processedFailure();
					onFailed(url);
				}
				// 
				try {
					pendingPages.addPage(page);
				} catch (QueueException e) {
					log.warn("一个页面加入待处理队列时失败" + e.getMessage());
				}
			}
		}
	}
	
	public static void main(String[] args) {
	}
}