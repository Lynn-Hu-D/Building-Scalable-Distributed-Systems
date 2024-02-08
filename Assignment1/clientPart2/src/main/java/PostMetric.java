public class PostMetric {
  private long start;
  private long end;
  private long latency;

  public PostMetric(long start, long end, long latency) {
    this.start = start;
    this.end = end;
    this.latency = latency;
  }

  public long getStart() {
    return start;
  }

  public void setStart(long start) {
    this.start = start;
  }

  public long getEnd() {
    return end;
  }

  public void setEnd(long end) {
    this.end = end;
  }

  public long getLatency() {
    return latency;
  }

  public void setLatency(long latency) {
    this.latency = latency;
  }
}
