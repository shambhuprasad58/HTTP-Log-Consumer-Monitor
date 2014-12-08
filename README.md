HTTP log monitoring console program

Create a simple console program that monitors HTTP traffic on your machine:
•	Consume an actively written-to w3c-formatted HTTP access log
•	Every 10s, display in the console the sections of the web site with the most hits (a section is defined as being what's before the second '/' in a URL. i.e. the section for "http://my.site.com/pages/create' is "http://my.site.com/pages"), as well as interesting summary statistics on the traffic as a whole.
•	Make sure a user can keep the console app running and monitor traffic on their machine
•	Whenever total traffic for the past 2 minutes exceeds a certain number on average, add a message saying that “High traffic generated an alert - hits = {value}, triggered at {time}”
•	Whenever the total traffic drops again below that value on average for the past 2 minutes, add another message detailing when the alert recovered
•	Make sure all messages showing when alerting thresholds are crossed remain visible on the page for historical reasons.
•	Write a test for the alerting logic
•	Explain how you’d improve on this application design


Classes

1.	HttpLogConsumerMonitor
This the class with the main method. It declares and initializes all the shared variables (Blocking queues). It initiates the sniffer and user interface in separate threads and waits on sniffer thread.
2.	Sniffer
Sniffer class runs on a separate thread from main thread. It waits for any update in user preference, accepts user preferences from preferenceQueue and restarts any thread or timer for collecting logs, generating alerts or getting the most hits statistics.

Helper Methods:
•	restartLogging: Kills any old thread that is reading the logs and restarts the logging with new preferences.
•	restartMostHitsCalculator: Kills any old thread that is calculating most hits and restarts the thread with new preferences.
•	restartAlertTracker: Kills any old thread that is sending the alerts and restarts the thread with new preferences.

3.	LogReader
It runs on a separate thread than the Sniffer which calls this thread. This class implements all methods and inner classes to read logs from logFile as soon as they arrive. It uses WatchService class to check for any new updates in the logging folder and reads new logs if available.
Moreover, it also sends alerts if the number of alerts cross the acceptable threshold. Also, it updates the stats as soon as new alert comes.

Helper Methods:
•	skipOldLogs
•	start
•	stop
•	updateOffset: Reads new logs from file
•	linesAvailable
•	getLine
•	hasEnded
•	updateStats: Update the stats as soon as new log comes.

Inner Class
•	SendStats: It forwards the updated stats to UI thread using the statsQueue Blocking queue.
•	FileWatcher: Handles any update in log file.

4.	MostHitsCalculator
This again runs on a separate thread than the Sniffer class that calls this. This class accepts new hits and calculates the most hit sections of url in the past x seconds where x is user input. Currently it is configured to give top 10 sections. New hits are read through the Blocking queue mostHitsURLQueue and result is forwarded to UI thread using mostHitsTopURL.

Helper Method:
•	removeMinimum: Removes the minimum hit url from the array list of urls. Final array list with top 10 entries is sent to the UI thread.

5.	AlertTracker
This class too runs on a separate thread. It keeps track of alerts that pass the limited time for which they need to	 be counted and removes them from the queue. When the queue size decreases below threshold, it sends an alert to UI thread with new value.
6.	HomeFrame
This is the UI class that runs on a separate thread than main. It has 4 parts. 3 parts display output (alerts, most hit sections and statistics) and last part takes user input. User input includes log file path, time period for calculating most hits section, time frame size for alerts and threshold value for number of alerts in that time frame.

Helper methods:
•	initializeAll: Initializes all UI components that need to be initialized.
•	userPreferenceUpdateButtonActionPerformed: Button action when user want to start or update the run. It validates the user input and sends a Preference object to sniffer class through preferencesQueue.
•	inputValidator
•	fileChooserButtonActionPerformed: Button action to choose the log file.
•	updateNewHits: Runs periodically on separate thread and calls other helper methods (updateMostHitsTable, updateStatsTable, postAlerts) to •	update the results (most hit sections, alerts and stats)
•	updateMostHitsTable
•	updateStatsTable
•	postAlerts
•	addAlertRow: Adds a new panel in the scroll pane where all the alerts are shown.
•	begin: starts the UI thread.

JUnit Test

•	AlertLogicTest: Initiates the LogReader and AlertTracker class, alert time frame and threshold. Generates requests above threshold and checks for an alert. Keeps the traffic above threshold and asserts for correct alerts. Finally after the alert time frame is over, checks for normal level alert.

Log File sample entry

line:127.0.0.1 - - [08/Dec/2014:20:17:40 +0530] "POST /xampp/ HTTP/1.1" 200 874
