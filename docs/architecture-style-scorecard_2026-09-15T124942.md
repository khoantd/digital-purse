# Architecture style scorecard\_2026-09-15 12:49

> AI-generated Richards architecture style scorecard for **E-Wallet**. Review and refine before treating as a decision record.
> Selection advice only — capture the chosen style as an ADR when ready.



# Architecture Style Scorecard — E-Wallet  
  
## Drivers (ranked)  
1. Simplicity — Ensures ease of onboarding for new engineers.  
2. Deployability — Facilitates frequent and reliable releases to production.  
3. Fault Tolerance — Maintains service availability during component failures.  
  
## Candidates considered  
- Layered: Default style for simple applications, but may lack deployability.  
- Modular Monolith: Provides a balance between simplicity and fault tolerance but may challenge deployability.  
- Service-Based: Allows for some modularity and better deployability but could increase complexity.  
    
## Scoring (driver columns only)  
  
| Style           | Simplicity | Deployability | Fault Tolerance | Total | Feasibility |  
|-----------------|------------|---------------|------------------|-------|-------------|  
| Layered         | ★★★★★    | ★☆☆☆☆      | ★☆☆☆☆          | 7     | ✅          |  
| Modular Monolith | ★★★★☆    | ★★★☆☆      | ★★☆☆☆          | 11    | ✅          |  
| Service-Based    | ★★★☆☆    | ★★★★☆      | ★★★☆☆          | 13    | ⚠️          |  
  
## Shortlist  
  
### 1. Modular Monolith  
- \*\*Wins on:\*\* Simplicity, Fault Tolerance  
- \*\*Trades away:\*\* Slightly reduced Deployability compared to Service-Based  
- \*\*Feasibility:\*\* Fits within the team size; basic CI/CD present, moderate budget allows for this architectural complexity.  
- \*\*Regret scenario:\*\* If chosen, it may fail due to challenges in team adaptation to modular complexities.  
  
### 2. Service-Based  
- \*\*Wins on:\*\* Deployability, Fault Tolerance  
- \*\*Trades away:\*\* Increased complexity in understanding overall architecture and potential onboarding issues.  
- \*\*Feasibility:\*\* Team size is just adequate; however, ops maturity may limit optimal performance of this design.  
- \*\*Regret scenario:\*\* If chosen, it may be prove cumbersome due to the unforeseen operational overhead and maintenance effort.  
  
## Recommendation  
If simplicity is paramount for rapid development, consider choosing the Modular Monolith. However, if deployability is a higher priority, the Service-Based architecture could be explored, keeping in mind that it introduces more complexity.  
  
## Next step  
Hand off to \`software-architecture-mastery\` to capture the chosen style as an ADR.
