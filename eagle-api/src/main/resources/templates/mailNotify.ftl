<div>
    <table border="1" class="t1">
        <caption>预警平台报警</caption>
        <tbody>
            <tr align="center">
                <td>任务名</td>
                <td>${policy.policyName}</td>
            </tr>
            <tr align="center">
                <td>sql语句</td>
                <td style="word-break:break-all">
                <pre>${policy.cql}</pre>
                </td>
            </tr>
            <tr align="center">
                <td>告警级别</td>
                <td>${policy.alertLevel}</td>
            </tr>
            <tr align="left">
                <td>命中结果</td>
                <td>
                    <p>${dataSink.data}</p>
                </td>
            </tr>
        </tbody>
    </table>
</div>