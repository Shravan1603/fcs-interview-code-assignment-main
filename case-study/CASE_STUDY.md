# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

**Key challenges:**

1. **Shared resource attribution**: When multiple Warehouses serve multiple Stores, allocating shared costs (e.g., transportation fleet, central management overhead) becomes ambiguous. Which warehouse bears the cost when a delivery truck serves two stores on one route? A cost allocation model is needed -- activity-based costing (ABC) is often a good fit here because it ties costs to measurable activities (picking, packing, shipping) rather than arbitrary percentages.

2. **Granularity vs. practicality**: Tracking costs at the individual shipment or order level gives the most accurate picture but is operationally expensive. The right granularity depends on what decisions the data will inform. If the goal is warehouse-level P&L, aggregating at the warehouse-month level may suffice. If the goal is per-product profitability, you need order-line-level tracking.

3. **Data consistency across systems**: Costs originate in different systems -- payroll (labor), WMS (inventory movements), TMS (transportation), ERP (overhead). These systems have different update cadences and data models. Reconciliation is a recurring challenge.

4. **Temporal alignment**: Costs and the activities that generated them often don't align in time. Inventory might arrive in January but the invoice is processed in February. Accrual-based accounting can address this but adds complexity.

**Questions I would ask before scoping:**

- How are costs currently tracked? Is there an existing ERP or finance system in place?
- What decisions should cost data inform? (pricing, warehouse closure, store allocation changes?)
- What is the current volume of transactions and how many warehouse-store relationships exist?
- Are there existing SLAs or cost targets per warehouse or store?
- Who are the consumers of cost reports -- finance, operations, or executive leadership?

---

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

**Potential strategies and expected outcomes:**

1. **Inventory placement optimization**: Analyze which Products sell most at which Stores, and position stock in the nearest Warehouse. This reduces transportation cost and delivery time. Expected outcome: 10-20% reduction in last-mile logistics cost.

2. **Warehouse utilization balancing**: The current model has capacity limits per location (maxCapacity). If some warehouses run at 30% utilization while others are at 95%, consolidation or redistribution of stock can reduce per-unit storage cost. Use the stock/capacity ratio from the Warehouse entity as a starting metric.

3. **Demand-driven replenishment**: Instead of fixed replenishment schedules, use actual sell-through data from Stores to trigger warehouse-to-store shipments. Reduces overstock (carrying cost) and stockouts (lost revenue).

4. **Transportation route optimization**: Batch deliveries from a Warehouse to multiple Stores in the same area. This is especially relevant given the location model -- warehouses in AMSTERDAM can serve multiple Amsterdam-area stores in consolidated trips.

**Prioritization approach:**

- Start with a data analysis phase: identify the top 3 cost categories by magnitude.
- Estimate savings potential and implementation effort for each strategy.
- Prioritize high-impact, low-effort items first (e.g., inventory redistribution before building a full TMS).
- Implement incrementally with measurement: define baseline metrics, implement a change, measure the delta, adjust.

**Key question:** What percentage of total cost does each category (labor, inventory, transport, overhead) represent? Without this, we risk optimizing a 5% cost category while ignoring an 80% one.

---

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

**Why integration matters:**

- **Single source of truth**: Without integration, cost data lives in spreadsheets or separate databases, leading to conflicting numbers. Finance says warehouse X costs EUR 500k/month; operations says EUR 450k. The discrepancy erodes trust in both systems.
- **Timely decision-making**: If cost data arrives with a 2-week lag, managers can't react to cost overruns until the damage is done. Near-real-time integration enables proactive cost control.
- **Audit compliance**: Financial reporting requires traceable, reconcilable data. Manual data transfers introduce errors and make audits painful.

**Benefits:**

- Automated reconciliation between operational events (warehouse shipments) and financial records (invoices, GL entries).
- Consolidated reporting: warehouse operational KPIs alongside financial metrics in one dashboard.
- Faster month-end close: no manual data gathering from multiple systems.

**Ensuring seamless integration:**

1. **Define a canonical data model**: Agree on how cost entities (cost centers, GL accounts, cost categories) map between systems. This is the hardest and most important step.
2. **Choose the right integration pattern**: For near-real-time, event-driven integration (e.g., publishing cost events to a message broker like Kafka) is preferable to batch ETL. For reporting, a periodic sync to a data warehouse may suffice.
3. **Idempotency and error handling**: Financial data must be accurate. Design integrations to be idempotent (processing the same event twice doesn't double-count) and have clear retry/dead-letter-queue strategies.
4. **Monitoring and alerting**: Set up alerts for sync failures or data discrepancies exceeding a threshold.

**Questions I would ask:**

- What financial systems are currently in use (SAP, Oracle, NetSuite, custom)?
- What is the acceptable latency for cost data (real-time, hourly, daily)?
- Are there existing APIs or do we need file-based integration?

---

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

**Why it matters for fulfillment specifically:**

Fulfillment costs are highly variable -- they fluctuate with order volume, seasonal demand, fuel prices, and labor availability. Without forecasting, the company either over-provisions (wasting money on idle capacity) or under-provisions (causing service failures during peak periods).

**Design considerations:**

1. **Historical data as the foundation**: The system needs at least 12-18 months of historical cost data broken down by category, warehouse, and time period. The `createdAt` and `archivedAt` fields on the Warehouse entity hint at this -- they allow tracking the lifecycle and associating costs with specific time periods.

2. **Driver-based budgeting**: Rather than budgeting "warehouse X costs EUR 500k next quarter," model costs as a function of drivers: cost = f(order volume, SKU count, warehouse size, labor rate). This makes forecasts adaptive to changing conditions.

3. **Scenario modeling**: The system should support "what-if" analysis. For example: "If we open a new warehouse at EINDHOVEN-001 (capacity 70, as per our location model), what is the projected cost impact considering redistribution of stock from AMSTERDAM-001?"

4. **Variance tracking**: Compare actual costs against budget continuously, not just at quarter-end. Flag variances above a threshold for review.

5. **Seasonality and trends**: Fulfillment has predictable seasonal patterns (holiday peaks, summer lulls). The forecasting model should account for these.

**Key data inputs needed:**

- Historical cost data by warehouse, store, and cost category
- Volume forecasts (orders, units shipped)
- Known future events (warehouse openings/closures, pricing changes)
- External factors (fuel price indices, labor market data)

**Question:** Does the company have a planning cycle (annual budget with quarterly re-forecasts)? This determines how often the forecasting system needs to run and how the UI should be structured.

---

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

**Why preserving cost history is critical:**

1. **Continuity of the business unit**: Since the Business Unit Code (e.g., MWH.001) is reused, stakeholders (finance, operations) think of it as one continuous cost center. If the cost history of the old warehouse disappears, it creates a gap in trend analysis. Was the replacement justified by cost savings? You can't answer that without the historical baseline.

2. **Budget baseline for the new warehouse**: The old warehouse's cost profile (run rate, cost per unit, fixed vs. variable mix) is the most relevant benchmark for budgeting the new warehouse. Without it, budgets are guesswork.

3. **ROI calculation**: The decision to replace a warehouse is often justified by an expected ROI (lower costs, higher capacity, better location). Preserving historical costs allows measuring whether the replacement actually delivered the projected savings.

4. **Audit and compliance**: Financial records must be retained for regulatory periods (typically 7-10 years). Archiving the warehouse entity while preserving its cost associations satisfies this.

**How the current system design supports this:**

The `archivedAt` timestamp on the Warehouse entity is the right approach. By archiving rather than deleting, the old warehouse record (and all associated cost data linked by business unit code) remains queryable. The `getAll()` method in WarehouseRepository correctly filters to `archivedAt is null` for active operations, while archived records remain accessible for historical analysis.

**Cost control considerations during replacement:**

- **Transition costs**: Running two warehouses during the migration period doubles fixed costs temporarily. Budget for this overlap explicitly.
- **Stock transfer costs**: Moving the existing stock (the `stock` field in our model) from old to new warehouse has logistics costs.
- **Ramp-up inefficiency**: A new warehouse typically operates at lower efficiency initially (staff training, process optimization). Expect higher per-unit costs for the first 3-6 months.
- **Set a cost target**: Use the old warehouse's cost per unit as the target the new warehouse should reach within a defined period (e.g., 6 months post-replacement).

**Question:** Is the replacement driven by cost reduction, capacity expansion, or location strategy? Each motivation implies different cost control priorities and success metrics.

---

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
