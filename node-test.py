#!/usr/bin/python

from selenium.webdriver.common.desired_capabilities import DesiredCapabilities
from selenium import webdriver

driver = webdriver.Remote('http://127.0.0.1:4444/wd/hub', DesiredCapabilities.FIREFOX)

driver.quit()
