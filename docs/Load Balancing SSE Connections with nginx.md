## Load Balancing SSE Connections (Nginx-specific)

To cope with transient pods and long-lived SSE streams, use one of these Nginx-based strategies:

1. **Weighted Least Connections**  
   Routes new SSE handshakes to the pod with the fewest active streams.
   ```nginx
   upstream sse_upstream {
     least_conn;                                  # choose server with fewest connections
     server pod1:8082 weight=1;
     server pod2:8083 weight=1;
     server pod3:8084 weight=1;
   }
   server {
     listen 8090;
     location /api/notifications/stream/ {
       proxy_pass http://sse_upstream;
       proxy_http_version 1.1;
       proxy_set_header Connection "";            # keep SSE alive
       proxy_read_timeout 24h;                    # long timeout
     }
   }
   ```
   Pros: minimal effort, works out-of-the-box  
   Cons: only balances *new* connections, no rebalance of existing streams

2. **Sticky Sessions + Connection Caps**  
   Use `ip_hash` (or cookie) to sticky-affinitize, but enforce a max per-pod limit.
   ```nginx
   upstream sse_upstream {
     ip_hash;                                     # client sticks to same pod
     zone sse_zone 64k;                           # track connections
     server pod1:8082 max_conns=200;
     server pod2:8083 max_conns=200;
     server pod3:8084 max_conns=200;
   }
   server {
     listen 8090;
     location /api/notifications/stream/ {
       proxy_pass http://sse_upstream;
       proxy_http_version 1.1;
       proxy_set_header Connection "";
       limit_conn sse_zone 200;                   # cap connections per pod
     }
   }
   ```
   Pros: prevents overload, keeps affinity for reconnections  
   Cons: uneven long-term if pods churn, draining old streams required

3. **Client-Hinted Rebalancing via Lua**  
   Dynamically suggest clients migrate to under-loaded pods.
   ```nginx
   http {
     lua_shared_dict pod_loads 1m;
     init_worker_by_lua_block {
       -- fetch /metrics/sse and populate pod_loads
     }
     upstream sse_upstream {
       server pod1:8082;
       server pod2:8083;
       server pod3:8084;
       balancer_by_lua_block {
         -- choose least-loaded pod from pod_loads
       }
     }
   }
   server {
     listen 8090;
     location /api/notifications/stream/ {
       proxy_pass http://sse_upstream;
       proxy_http_version 1.1;
       proxy_set_header Connection "";
       header_filter_by_lua_block {
         if ngx.var.upstream_status == "503" then
           ngx.header["X-Rebalance-Hint"] = "pod2"  -- example
         end
       }
     }
   }
   ```
   Pros: can rebalance *existing* streams gracefully  
   Cons: adds Lua complexity, clients must honor hints

4. **Consistent Hashing with Nginx `hash`**  
   Map user-ID or client-ID → virtual node → pod to minimize remaps.
   ```nginx
   upstream sse_upstream {
     hash $cookie_user_id consistent;             # or $arg_userId
     server pod1:8082;
     server pod2:8083;
     server pod3:8084;
   }
   server {
     listen 8090;
     location /api/notifications/stream/ {
       proxy_pass http://sse_upstream;
       proxy_http_version 1.1;
       proxy_set_header Connection "";
     }
   }
   ```
   Pros: stable distribution even under scaling events  
   Cons: requires user ID via cookie/param, more complex to tweak

---

**Recommendation**  
- **Quick Win**: Start with **Weighted Least Connections**.  
- **Medium Effort**: Add **Sticky + Caps** if you already use `ip_hash`.  
- **Long-Term**: Explore **Lua-driven hints** or **Consistent Hashing** for true resilience.  