#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from '@aws-cdk/core';
import {MicronautKotlinBlancoSampleStack} from '../lib/micronaut-kotlin-blanco-sample-stack';

const app = new cdk.App();
new MicronautKotlinBlancoSampleStack(app, 'MicronautKotlinBlancoSampleStack', {
    env: {
        // AWS アカウントとリージョンを指定する環境変数は、
        // 以下のように明示的に設定しないと、有効にならない。
        account: process.env.CDK_DEFAULT_ACCOUNT,
        region: process.env.CDK_DEFAULT_REGION
    }
});
