# ---------------------------------------------------------------------------
# Privater Artefakt-Bucket: Pipeline legt hier die Release-Jars ab, die Instanz
# holt sie ueber das Instance Profile. Funktioniert auch bei privatem Repo.
# ---------------------------------------------------------------------------
# Learner Lab: eine SCP verbietet s3:GetBucketObjectLockConfiguration. Die Ressource "aws_s3_bucket"
# liest diese Einstellung bei jedem Refresh und scheitert (Provider 5.x und 6.x). Deshalb werden NUR die
# beiden Buckets per CloudFormation-Stack erstellt (eindeutige Namen generiert CloudFormation, wie bucket_prefix);
# alle weiteren Einstellungen sind normale OpenTofu-Ressourcen.
resource "aws_cloudformation_stack" "buckets" {
  name = "${local.name}-buckets"
  template_body = jsonencode({
    AWSTemplateFormatVersion = "2010-09-09"
    Resources = {
      Artifacts = { Type = "AWS::S3::Bucket", DeletionPolicy = "Delete" }
      Frontend  = { Type = "AWS::S3::Bucket", DeletionPolicy = "Delete" }
    }
    Outputs = {
      ArtifactsBucket = { Value = { Ref = "Artifacts" } }
      FrontendBucket  = { Value = { Ref = "Frontend" } }
      FrontendArn     = { Value = { "Fn::GetAtt" = ["Frontend", "Arn"] } }
    }
  })
}

locals {
  artifacts_bucket = aws_cloudformation_stack.buckets.outputs["ArtifactsBucket"]
  frontend_bucket  = aws_cloudformation_stack.buckets.outputs["FrontendBucket"]
  frontend_arn     = aws_cloudformation_stack.buckets.outputs["FrontendArn"]
}

resource "aws_s3_bucket_public_access_block" "artifacts" {
  bucket                  = local.artifacts_bucket
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# ---------------------------------------------------------------------------
# Oeffentlicher Frontend-Bucket (S3 Static Website)
# Hier darf nur liegen, was jeder sehen darf: HTML/CSS/JS, keine Secrets, keine Jars.
# ---------------------------------------------------------------------------
resource "aws_s3_bucket_ownership_controls" "frontend" {
  bucket = local.frontend_bucket
  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket                  = local.frontend_bucket
  block_public_acls       = true
  ignore_public_acls      = true
  block_public_policy     = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_policy" "frontend_public_read" {
  bucket     = local.frontend_bucket
  depends_on = [aws_s3_bucket_public_access_block.frontend]

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "PublicReadGetObject"
      Effect    = "Allow"
      Principal = "*"
      Action    = "s3:GetObject"
      Resource  = "${local.frontend_arn}/*"
    }]
  })
}

resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = local.frontend_bucket
  index_document {
    suffix = "index.html"
  }
}

# Nur die Infrastruktur kennt die Adresse der Instanz -> OpenTofu schreibt die Frontend-Konfiguration.
# Das Frontend-Deployment schliesst config.js aus (aws s3 sync --exclude config.js) und ueberschreibt sie nie.
resource "aws_s3_object" "frontend_config" {
  bucket        = local.frontend_bucket
  key           = "config.js"
  content_type  = "application/javascript"
  cache_control = "no-cache"
  content       = <<-EOT
    // Von OpenTofu generiert - nicht von Hand aendern, wird beim Frontend-Deployment nicht ueberschrieben.
    window.TICTACTEST_CONFIG = {
      consoleUrl: "http://${aws_eip.tictactest.public_dns}/",
      versionUrl: "http://${aws_eip.tictactest.public_dns}/version.txt"
    };
  EOT
}
