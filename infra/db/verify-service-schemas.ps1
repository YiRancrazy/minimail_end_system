$ErrorActionPreference = "Stop"

$workspace = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$migrationRoot = Join-Path $workspace "minimall-flyway-core/src/main/resources/db/migration"
$services = @{
    auth     = @{ Schema = "auth_db"; FlywayConfig = "minimall-auth-service/src/main/resources/application.yml"; DataSourceConfig = "minimall-auth-service/src/main/resources/application.yml" }
    user     = @{ Schema = "user_db"; FlywayConfig = "minimall-user-service/src/main/resources/application.yml"; DataSourceConfig = "minimall-user-service/src/main/resources/application.yml" }
    merchant = @{ Schema = "merchant_db"; FlywayConfig = "minimall-merchant-service/src/main/resources/application.yml"; DataSourceConfig = "minimall-merchant-service/src/main/resources/application.yml" }
    goods    = @{ Schema = "goods_db"; FlywayConfig = "minimall-goods-service/src/main/resources/application.yml"; DataSourceConfig = "minimall-goods-service/src/main/resources/application.yml" }
    cart     = @{ Schema = "cart_db"; FlywayConfig = "minimall-cart-service/src/main/resources/application.yml"; DataSourceConfig = "minimall-cart-service/src/main/resources/application.yml" }
    order    = @{ Schema = "order_db"; FlywayConfig = "minimall-order-service/src/main/resources/application.yml"; DataSourceConfig = "minimall-order-service/src/main/resources/application.yml" }
    stock    = @{ Schema = "stock_db"; FlywayConfig = "minimall-stock-service/src/main/resources/application.yml"; DataSourceConfig = "minimall-stock-service/src/main/resources/application.yml" }
    pay      = @{ Schema = "pay_db"; FlywayConfig = "minimall-pay-service/src/main/resources/application.yml"; DataSourceConfig = "minimall-pay-service/src/main/resources/application.yml" }
    notify   = @{ Schema = "notify_db"; FlywayConfig = "minimall-notify-service/src/main/resources/application.yml"; DataSourceConfig = "minimall-notify-service/src/main/resources/application.yml" }
    id       = @{ Schema = "id_db"; FlywayConfig = "minimall-id-service/src/main/resources/application.yml"; DataSourceConfig = "minimall-id-service/src/main/resources/application-dev.yml" }
}
$allowedTables = @{
    auth     = @("t_auth_role", "t_auth_permission", "t_auth_user", "t_auth_role_permission", "t_auth_token_blacklist")
    user     = @("t_user", "t_user_address", "t_user_favorite")
    merchant = @("t_merch_merchant", "t_merch_shop")
    goods    = @("t_goods_spu", "t_sku", "t_goods_audit_record")
    cart     = @("t_cart_item")
    order    = @("t_order", "t_order_item", "t_order_logistics", "t_order_status_log",
                 "t_order_payment_snapshot", "t_order_outbox")
    stock    = @("t_stock", "t_stock_journal", "t_stock_transfer", "t_stock_count_task")
    pay      = @("t_pay_transaction", "t_pay_refund")
    notify   = @("t_notify_message", "t_notify_complaint", "t_notify_comment", "t_notify_preference")
    id       = @("t_id_segment")
}

foreach ($domain in $services.Keys) {
    $service = $services[$domain]
    $flywayConfigPath = Join-Path $workspace $service.FlywayConfig
    $flywayConfig = [IO.File]::ReadAllText($flywayConfigPath, [Text.Encoding]::UTF8)
    if ($flywayConfig -notmatch "locations: classpath:db/migration/$domain") {
        throw "$domain Flyway location mismatch"
    }
    $dataSourceConfigPath = Join-Path $workspace $service.DataSourceConfig
    $dataSourceConfig = [IO.File]::ReadAllText($dataSourceConfigPath, [Text.Encoding]::UTF8)
    if ($dataSourceConfig -notmatch "/$($service.Schema)\?") {
        throw "$domain datasource points to an unexpected schema"
    }

    $domainDir = Join-Path $migrationRoot $domain
    $migrationText = (Get-ChildItem -File $domainDir -Filter "*.sql" |
        ForEach-Object { [IO.File]::ReadAllText($_.FullName, [Text.Encoding]::UTF8) }) -join "`n"
    $tables = [regex]::Matches($migrationText, "\bt_[a-z0-9_]+\b") |
        ForEach-Object { $_.Value } |
        Sort-Object -Unique
    $foreignTables = $tables | Where-Object { $_ -notin $allowedTables[$domain] }
    if ($foreignTables) {
        throw "$domain migrations contain foreign tables: $($foreignTables -join ', ')"
    }
}

$undoFiles = Get-ChildItem -Recurse -File $migrationRoot -Filter "*.sql" |
    Where-Object { (Get-Content -Raw $_.FullName) -match "CREATE TABLE IF NOT EXISTS ``undo_log``" }
$undoDomains = $undoFiles | ForEach-Object { Split-Path $_.DirectoryName -Leaf } | Sort-Object
if (($undoDomains -join ",") -ne "order,pay,stock") {
    throw "Expected undo_log in order, stock and pay schemas"
}

Write-Host "Service schema verification passed."
