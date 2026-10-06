package tsp.run;

import java.awt.BorderLayout;
import java.awt.Dimension;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * Plots the score over time
 */
public class MonitorChart
{
	private static MonitorChart instance = null;
	private TimeSeries bestEvaluation;
	private TimeSeries currentEvaluation;
	
	/**
	 * @return the current instance
	 */
	public static MonitorChart getInstance ()
	{
		MonitorChart instance = MonitorChart.instance;
		if (instance == null)
			instance = new MonitorChart ("");
		return instance;
	}
	
	/**
	 * @param title the chart title
	 * @param visible whether to show the chart
	 * @return a new instance
	 */
	public static MonitorChart getNewInstance (String title)
	{
		MonitorChart.instance = new MonitorChart (title);
		return MonitorChart.instance;
	}
	
	private MonitorChart (String title)
	{
		if (Main.DISPLAY_CHART)
		{
			this.bestEvaluation = new TimeSeries ("Best evaluation");
			this.currentEvaluation = new TimeSeries ("Current evaluation");
			TimeSeriesCollection tsc = new TimeSeriesCollection ();
			tsc.addSeries(this.currentEvaluation);
			tsc.addSeries(this.bestEvaluation);
			JFreeChart chart = ChartFactory.createTimeSeriesChart (title, "Time", "Fitness", tsc);
			ChartPanel chartPanel = new ChartPanel (chart);
			chartPanel.setPreferredSize (new Dimension (600, 300));
			MainFrame mainFrame = MainFrame.getInstance ();
			mainFrame.add (chartPanel, BorderLayout.NORTH);
			mainFrame.pack ();
		}
	}
	
	/**
	 * Adds a data point to the chart
	 * @param current the current solution's score
	 * @param best the best score so far
	 */
	public void addData (double current, double best)
	{
		if (Main.DISPLAY_CHART)
		{
			try
			{
				Millisecond now = new Millisecond ();
				this.currentEvaluation.add (now, current);
				this.bestEvaluation.add (now, best);
			}
			catch (Exception e)
			{			
			}
		}
	}
}
