
@Grab('com.amazonaws:aws-java-sdk:1.8.2')
import com.amazonaws.auth.*
import com.amazonaws.services.s3.*

def configPath = "./config.groovy"

if (!new File(configPath).exists()){
  System.err.println("Configuration file ${configPath} not exists")
  System.exit(-1)
}

def config = new ConfigSlurper().parse(new File(configPath).toURL())

String accessKey = config.aws.credentials.accessKey
String secretKey = config.aws.credentials.secretKey
String bucketName = config.aws.s3.bucket

def cli = new CliBuilder(usage: 's3upload [options] dir')
cli.h args: 0, longOpt: 'help', 'print usage information'
cli.bucket args: 1, argName: 'bucketName', 'use given bucket name'

def options = cli.parse(args)

if (options.h){
  cli.usage()
  System.exit(0)
}

if (options.bucket){
  bucketName = options.bucket
}

if (options.arguments().size() == 0){
  System.err.println "No dir specified. Use -h to show usage information"
  System.exit(-1)
}

def targetDir = options.arguments().first()

println "Processing target dir: ${targetDir}"

if (!new File(targetDir).isDirectory()){
  System.err.println "Target dir ${targetDir} is not valid"
  System.exit(-1)
}

def credentials = new BasicAWSCredentials(accessKey, secretKey)
def s3client = new AmazonS3Client(credentials)

def zipFile = File.createTempFile("temp", ".zip")
zipFile.delete()

new AntBuilder().zip(destFile: zipFile, basedir: targetDir)

println "\n Your current buckets: \n ----------------- \n"
s3client.listBuckets().each {
  println it.name
}
println "-----------------"

if (!s3client.doesBucketExist(bucketName)){
  s3client.createBucket(bucketName)
}

s3client.putObject(bucketName, "${new File(targetDir).name}.zip", zipFile)

println "Finish uploading zip file ${targetDir}.zip"
