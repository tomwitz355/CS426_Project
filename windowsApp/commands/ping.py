from pythonping import ping

def main():
    response = ping('google.com', count=10)
    print(response.rtt_avg_ms)


main()
