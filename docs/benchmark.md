# 缓存优化压测报告

> 目标：验证 `item-service` 开启三级缓存前后，在相同机器、相同数据、相同接口下的 QPS 与响应时间差异。
>
> 结论先行：**并发 30 时 QPS 330 → 1,156（3.5×），P95 167 ms → 32 ms（下降 81%）**。

![QPS 与 P95 对比](assets/benchmark/qps-latency.png)

## 1. 测试环境

| 项目 | 配置 |
|---|---|
| CPU | AMD Ryzen 5 3550H，4 核 8 线程，2.1 GHz |
| 内存 | 16 GB（14.9 GB 可用） |
| 操作系统 | Windows 11 家庭中文版 |
| JDK | OpenJDK 17.0.9 |
| 应用 | user / item / activity / info / gateway 五个 Spring Boot 进程 |
| MySQL | 9.7.1（VMware 虚拟机，端口 3307） |
| Redis | 6.2.23（VMware 虚拟机，端口 6379） |
| 压测客户端 | Python 3.13 + aiohttp，与本机应用同机运行 |

> MySQL 9.7.1 是本次实验环境的实际版本；项目 SQL 面向 MySQL 8.0+，两者在本项目使用的语法范围内兼容。

## 2. 测试对象

### 2.1 接口

```text
GET http://localhost:8082/items/page?pageNo=1&pageSize=8
```

选择**直连 `item-service`** 而不是经过网关，原因：

- 网关、Feign、用户服务都会引入额外排队，容易掩盖缓存层本身的差异；
- 该接口在缓存命中后不需要访问 MySQL，能最直接地观察「数据库查询 vs Redis 查询」的差距；
- 关闭缓存时，同一接口每次都要执行 `COUNT + LIMIT` 分页查询。

### 2.2 两种版本

| 版本 | 代码 | 行为 |
|---|---|---|
| 关闭缓存（基线） | [`benchmark/no-cache`](https://github.com/chuqiuqiu/campus-life/tree/benchmark/no-cache) | `queryItemPage` 的 `cacheable=false`；`queryWithCache` 直接 `getById` 查 MySQL；仍保留浏览量计数、Feign 等非缓存逻辑 |
| 开启缓存（优化后） | `master` | 列表走 Redis 列表缓存；详情走 Caffeine → Redis → MySQL 三级缓存 |

> 基线分支只关闭 item 的业务缓存，不是“关闭 Redis”。这样对比的是缓存方案本身，而不是把所有 Redis 功能一起拿掉。

## 3. 压测方法

- **模型**：closed-loop，固定并发数，每个 worker 在收到响应后立即发起下一次请求；
- **连接**：HTTP keep-alive，`aiohttp.TCPConnector(limit=并发数)`；
- **并发梯度**：1、10、30、60、100；
- **单轮时长**：20 秒；
- **预热**：每档并发先跑 5 秒，丢弃预热样本（同时完成 JIT / 连接池 / 缓存预热）；
- **重复**：每档 2 轮，按 QPS 取中位数；
- **指标**：QPS、平均延迟、P50 / P90 / P95 / P99、最大延迟、错误率；
- **错误判定**：HTTP 非 200 或连接异常均计为错误；
- **压测脚本**：[`benchmark/load_test.py`](../benchmark/load_test.py)。

## 4. 结果

### 4.1 汇总

| 并发 | 版本 | QPS | 平均 RT | P50 | P90 | P95 | P99 | 错误率 |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | 关闭缓存 | 81 | 12.3 ms | 11.3 ms | 16.8 ms | 20.2 ms | 27.8 ms | 0% |
| 1 | 开启缓存 | 157 | 6.4 ms | 5.9 ms | 8.6 ms | 9.8 ms | 13.6 ms | 0% |
| 10 | 关闭缓存 | 291 | 34.4 ms | 29.2 ms | 50.7 ms | 62.9 ms | 107.0 ms | 0% |
| 10 | 开启缓存 | 828 | 12.1 ms | 11.5 ms | 15.2 ms | 17.0 ms | 24.6 ms | 0% |
| 30 | 关闭缓存 | 330 | 90.7 ms | 81.1 ms | 142.4 ms | 166.7 ms | 216.0 ms | 0% |
| 30 | 开启缓存 | **1,156** | 25.9 ms | 25.4 ms | 30.1 ms | **32.3 ms** | 50.7 ms | 0% |
| 60 | 关闭缓存 | 359 | 166.9 ms | 157.2 ms | 244.4 ms | 283.2 ms | 377.0 ms | 0% |
| 60 | 开启缓存 | 1,144 | 52.4 ms | 51.3 ms | 58.5 ms | 63.9 ms | 91.6 ms | 0% |
| 100 | 关闭缓存 | 341 | 292.0 ms | 269.2 ms | 463.3 ms | 521.6 ms | 638.3 ms | 0% |
| 100 | 开启缓存 | 1,143 | 87.3 ms | 85.2 ms | 95.6 ms | 102.5 ms | 135.4 ms | 0% |

### 4.2 关键结论

- **峰值 QPS**：359（关闭缓存，60 并发）→ 1,156（开启缓存，30 并发），提升 **3.2×**；
- **并发 30**：QPS 提升 **3.5×**，P95 降低 **81%**，平均延迟降低 **71%**；
- **并发 100**：QPS 提升 **3.4×**，P95 降低 **80%**；
- 关闭缓存时，并发超过 30 后 QPS 基本不再增长，延迟持续上升，说明 MySQL 连接池/查询成为瓶颈；
- 开启缓存后，QPS 在 30~100 并发区间稳定在 1,140~1,156，说明瓶颈转移到应用 CPU / 压测客户端，而不是数据库。

### 4.3 原始数据

- [关闭缓存汇总](../benchmark/results/baseline.json)
- [开启缓存汇总](../benchmark/results/optimized.json)
- [两轮原始样本（CSV）](../benchmark/results/runs.csv)

`baseline.json` / `optimized.json` 保存每档的中位数汇总，`runs.csv` 保存两轮完整样本；汇总表按两轮 QPS 中位数取值。

## 5. 如何复现

### 5.1 运行压测

确保 item-service 已启动、数据库与 Redis 可用：

```bash
python benchmark/load_test.py \
  --url "http://localhost:8082/items/page?pageNo=1&pageSize=8" \
  --levels 1,10,30,60,100 \
  --duration 20 \
  --warmup 5 \
  --repeat 2 \
  --output benchmark/results/my-run.json
```

### 5.2 复现基线

```bash
git switch benchmark/no-cache
mvn -pl item-service -am package -DskipTests
java -jar item-service/target/item-service.jar
```

运行同一组 `load_test.py` 命令，再切回 `master` 重新构建、启动优化版本即可。

### 5.3 重新生成图表

```bash
python benchmark/render_charts.py
```

输出：

- `docs/assets/benchmark/qps-latency.png`

## 6. 注意事项

1. **这是单机压测**：应用、压测客户端、VMware 虚拟机共享同一台笔记本的 CPU 和内存，绝对 QPS 不能直接外推到生产环境；
2. **缓存是热的**：每档并发开始前有 5 秒预热，测的是缓存命中后的稳态吞吐；冷启动/缓存穿透场景需要单独测；
3. **客户端可能先成为瓶颈**：Python 单进程 async 客户端在 1,100+ QPS 时也会消耗 CPU，因此优化后的曲线在 30 并发后趋于平台；
4. **基线仍保留非缓存逻辑**：浏览量 Redis 计数、Feign 调用等没有关闭，保证对比只聚焦业务缓存；
5. **结论看相对值**：建议重点关注同一档并发下的 QPS 倍数与 P95 降低比例，而不是绝对数字。